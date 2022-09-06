package com.javabgy.paymentdemo.service.impl;

import com.google.gson.Gson;
import com.javabgy.paymentdemo.config.WxPayConfig;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.entity.RefundInfo;
import com.javabgy.paymentdemo.enums.OrderStatus;
import com.javabgy.paymentdemo.enums.PayType;
import com.javabgy.paymentdemo.enums.wxpay.WxApiType;
import com.javabgy.paymentdemo.enums.wxpay.WxNotifyType;
import com.javabgy.paymentdemo.enums.wxpay.WxTradeState;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.service.PaymentInfoService;
import com.javabgy.paymentdemo.service.RefundInfoService;
import com.javabgy.paymentdemo.service.WxPayService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信支付Service实现
 *
 * @author Gary
 * @date 2022/9/3 下午6:02
 **/
@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private CloseableHttpClient wxPayNoSignClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Map<String, Object> nativePay(Long productId) throws Exception {

        log.info("生成订单");
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.WXPAY.getType());

        String codeUrl = orderInfo.getCodeUrl();
        if (!StringUtils.isEmpty(codeUrl)) {
            log.info("订单已存在，二维码已保存");
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        }

        // 调用统一下单API
        log.info("调用统一下单API");
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        // 请求body参数
        Gson gson = new Gson();
        Map paramsMap = new HashMap();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));

        Map amountMap = new HashMap();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);
        String jsonParams = gson.toJson(paramsMap);

        log.info("请求参数: {}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("请求成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("请求成功");
            } else {
                System.out.println("Native下单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new IOException("request failed");
            }
            // 响应结果
            Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);
            codeUrl = resultMap.get("code_url");

            // 保存二维码
            orderInfoService.saveOrderCodeUrl(orderInfo.getOrderNo(), codeUrl);

            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        } finally {
            response.close();
        }
    }

    @Override
    public void processOrder(Map<String, Object> bodyMap) throws Exception {
        log.info("处理订单");

        // 解密报文
        String plainText = decryptFromResource(bodyMap);

        // 将明文转换为map
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                // 处理重复通知
                // 接口调用的幂等性
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    return;
                }

                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                // 记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                lock.unlock();
            }
        }

    }

    /**
     * 取消订单
     * @param orderNo:
     * @return void
     * @author: Gary
     * @date: 2022/9/5 上午10:34
     */
    @Override
    public void cancelOrder(String orderNo) throws Exception {

        // 调用微信支付的关单接口
        this.closeOrder(orderNo);

        // 更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);

    }

    @Override
    public String queryOrder(String orderNo) throws Exception {
        log.info("查询订单接口：{}", orderNo);
        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("请求成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("请求成功");
            } else {
                System.out.println("Native下单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new IOException("request failed");
            }
            return bodyAsString;
        } finally {
            response.close();
        }

    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     * 如果已支付则更新商户端订单状态
     * 如果未支付则调用微信关单接口，并更新商户端订单状态
     *
     * @param orderNo: 
     * @return void
     * @author: Gary
     * @date: 2022/9/5 上午11:48
     */
    @Override
    public void checkOrderStatus(String orderNo) throws Exception {
        log.warn("根据订单号核实订单状态：{}", orderNo);

        // 调用微信支付接口查询
        String result = this.queryOrder(orderNo);

        Gson gson = new Gson();
        Map resultMap = gson.fromJson(result, HashMap.class);

        Object tradeState = resultMap.get("trade_state");

        if (WxTradeState.SUCCESS.getType().equals(tradeState)) {
            log.warn("核实订单已支付：{}", orderNo);
            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录支付日志
            paymentInfoService.createPaymentInfo(result);
        }
        if (WxTradeState.NOTPAY.getType().equals(tradeState)) {
            log.warn("核实订单未支付：{}", orderNo);
            // 订单未支付调用，关单接口，更新本地订单状态
            this.closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    /**
     * 申请退款
     * @param orderNo:
     * @param reason:
     * @return void
     * @author: Gary
     * @date: 2022/9/5 下午3:39
     */
    @Override
    public void refund(String orderNo, String reason) throws Exception {
        log.info("创建退款单记录");
        // 根据订单编号创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundInfoByOrderNo(orderNo, reason);

        // 调用退款API
        log.info("调用退款API");
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        HttpPost httpPost = new HttpPost(url);
        // 请求body参数
        Gson gson = new Gson();
        Map paramsMap = new HashMap();
        paramsMap.put("out_trade_no", refundInfo.getOrderNo());
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", refundInfo.getReason());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map amountMap = new HashMap();
        amountMap.put("refund", refundInfo.getRefund());
        amountMap.put("total", refundInfo.getTotalFee());
        amountMap.put("currency", "CNY");

        paramsMap.put("amount", amountMap);
        String jsonParams = gson.toJson(paramsMap);

        log.info("请求参数: {}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("退款成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("退款成功");
            } else {
                System.out.println("Native下单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new RuntimeException("request failed");
            }

            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

            // 更新退款单状态
            refundInfoService.updateRefund(bodyAsString);

        } finally {
            response.close();
        }
    }

    /**
     * 查询退款
     * @param refundNo:
     * @return java.lang.String
     * @author: Gary
     * @date: 2022/9/5 下午4:30
     */
    @Override
    public String queryRefund(String refundNo) throws Exception {
        log.info("查询退款单接口：{}", refundNo);
        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("请求成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("请求成功");
            } else {
                System.out.println("查询退款单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new IOException("request failed");
            }
            return bodyAsString;
        } finally {
            response.close();
        }
    }

    @Override
    public void processRefund(Map<String, Object> bodyMap) throws Exception {
        log.info("处理退款单");
        // 解密报文
        String plainText = decryptFromResource(bodyMap);

        // 将明文转换为map
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        // 处理重复通知
        // 接口调用的幂等性
        if (lock.tryLock()) {
            try {
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!OrderStatus.REFUND_PROCESSING.getType().equals(orderStatus)) {
                    return;
                }

                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);

                // 更新退款单
                refundInfoService.updateRefund(plainText);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public String queryBill(String billDate, String type) throws Exception {
        log.warn("申请账单接口调用：{}", billDate);
        String url = "";
        if ("tradebill".equals(type)) {
            url = WxApiType.TRADE_BILLS.getType();
        } else if ("fundflowbill".equals(type)) {
            url = WxApiType.FUND_FLOW_BILLS.getType();
        } else {
            throw new RuntimeException("不支持的账单类型");
        }
        url = wxPayConfig.getDomain().concat(url).concat("?bill_date=").concat(billDate);


        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("请求成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("请求成功");
            } else {
                log.error("Native下单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new IOException("request failed");
            }
            // 响应结果
            Gson gson = new Gson();
            Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);
            return resultMap.get("download_url");

        } finally {
            response.close();
        }
    }

    @Override
    public String downloadBill(String billDate, String type) throws Exception {
        log.warn("下载账单：{}, {}", billDate, type);

        String downloadUrl = this.queryBill(billDate, type);

        HttpGet httpGet = new HttpGet(downloadUrl);
        httpGet.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayNoSignClient.execute(httpGet);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            String bodyAsString = EntityUtils.toString(response.getEntity());
            if (statusCode == 200) {
                log.info("下载账单成功，返回值 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("请求成功");
            } else {
                log.error("下载账单失败, 错误码 = " + statusCode + ",返回值 = " + bodyAsString);
                throw new IOException("request failed");
            }
            // 响应结果
            return bodyAsString;

        } finally {
            response.close();
        }
    }

    /**
     * 关单接口调用
     * @param orderNo:
     * @return void
     * @author: Gary
     * @date: 2022/9/5 上午10:37
     */
    private void closeOrder(String orderNo) throws Exception {
        log.info("关单接口调用，订单号：{}", orderNo);
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        // 创建远程请求对象
        HttpPost httpPost = new HttpPost(url);

        // 组装json请求体
        Gson gson = new Gson();
        Map<String, String> paramsMap = new HashMap<>(4);
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数：{}", jsonParams);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");

        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("请求成功200");
            } else if (statusCode == 204) {
                log.info("请求成功204");
            } else {
                System.out.println("Native下单失败, 错误码 = " + statusCode);
                throw new IOException("request failed");
            }

        } finally {
            response.close();
        }
    }

    /**
     * 对称解密
     * @param bodyMap:
     * @return java.lang.String
     * @author: Gary
     * @date: 2022/9/4 下午8:32
     */
    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("对称解密");

        // 通知数据
        Map<String, String> resourceMap = (Map) bodyMap.get("resource");

        // 获取数据密文
        String cipherText = resourceMap.get("ciphertext");
        // 获取随机数
        String nonce = resourceMap.get("nonce");
        // 获取附加数据
        String associatedData = resourceMap.get("associated_data");

        log.info("密文：{}", cipherText);
        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                cipherText);
        log.info("明文：{}", plainText);
        return plainText;
    }
}
