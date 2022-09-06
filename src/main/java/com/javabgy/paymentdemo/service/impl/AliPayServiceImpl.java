package com.javabgy.paymentdemo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.entity.RefundInfo;
import com.javabgy.paymentdemo.enums.OrderStatus;
import com.javabgy.paymentdemo.enums.PayType;
import com.javabgy.paymentdemo.enums.wxpay.AliTradeState;
import com.javabgy.paymentdemo.service.AliPayService;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.service.PaymentInfoService;
import com.javabgy.paymentdemo.service.RefundInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * alipay Service实现类
 *
 * @author Gary
 * @date 2022/9/5 下午10:39
 **/
@Service
@Slf4j
public class AliPayServiceImpl implements AliPayService {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private Environment config;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 支付宝下单
     *
     * @param productId:
     * @return java.lang.String
     * @author: Gary
     * @date: 2022/9/5 下午10:43
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String tradeCreate(Long productId) {
        log.info("生成订单");
        // 生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId, PayType.ALIPAY.getType());

        // 调用支付宝接口
        try {
            AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
            request.setNotifyUrl(config.getProperty("alipay.notify-url"));
            request.setReturnUrl(config.getProperty("alipay.return-url"));
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderInfo.getOrderNo());
            BigDecimal amount = new BigDecimal(orderInfo.getTotalFee().toString()).divide(new BigDecimal("100"));
            bizContent.put("total_amount", amount);
            bizContent.put("subject", orderInfo.getTitle());
            bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

            request.setBizContent(bizContent.toString());

            // 执行支付宝接口
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
            if (response.isSuccess()) {
                log.info("调用支付宝成功，返回结果：{}", response.getBody());
                return response.getBody();
            } else {
                log.info("调用支付宝失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
                throw new RuntimeException("创建支付交易失败");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("创建支付交易失败");
        }
    }

    /**
     * 支付宝订单处理
     *
     * @param params:
     * @return void
     * @author: Gary
     * @date: 2022/9/6 下午3:46
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(Map<String, String> params) {
        log.info("处理订单");
        String outTradeNo = params.get("out_trade_no");

        if (lock.tryLock()) {
            try {
                // 处理重复通知，接口幂等
                String orderStatus = orderInfoService.getOrderStatus(outTradeNo);
                if (!OrderStatus.NOTPAY.getType().equals(orderStatus)) {
                    return;
                }
                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(outTradeNo, OrderStatus.SUCCESS);
                // 记录支付日志
                paymentInfoService.createPaymentInfoForAliPay(params);

            } finally {
                lock.unlock();
            }
        }

    }

    @Override
    public void cancleOrder(String orderNo) {

        // 调用支付宝的统一收单交易关闭接口
        this.closeOrder(orderNo);
        // 更新用户订单状态

        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);

    }

    @Override
    public String queryOrder(String orderNo) {
        log.info("查询订单接口：{}", orderNo);

        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("查询订单接口成功，返回结果：{}", response.getBody());
                return response.getBody();
            } else {
                log.info("查询订单接口失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
//                throw new RuntimeException("查询订单接口失败");
                return null;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("查询订单接口失败");
        }
    }

    /**
     * 根据订单号查询微信支付查单接口，核实订单状态
     * 如果订单未创建，直接更新商户订单状态
     * 如果已支付则更新商户端订单状态
     * 如果未支付则调用支付宝关单接口，并更新商户端订单状态
     *
     * @param orderNo:
     * @return void
     * @author: Gary
     * @date: 2022/9/6 下午5:42
     */
    @Override
    public void checkOrderStatus(String orderNo) {
        log.warn("根据订单号核实订单状态：{}", orderNo);

        // 调用微信支付接口查询
        String result = this.queryOrder(orderNo);

        if (result == null) {
            log.warn("核实订单未创建，订单号：{}", orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }

        Gson gson = new Gson();
        Map<String, LinkedTreeMap> resultMap = gson.fromJson(result, HashMap.class);
        LinkedTreeMap response = resultMap.get("alipay_trade_query_response");
        String tradeStatus = (String) response.get("trade_status");

        if (AliTradeState.SUCCESS.getType().equals(tradeStatus)) {
            log.warn("核实订单已支付：{}", orderNo);
            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录支付日志
            paymentInfoService.createPaymentInfoForAliPay(response);
        }
        if (AliTradeState.NOTPAY.getType().equals(tradeStatus)) {
            log.warn("核实订单未支付：{}", orderNo);
            // 订单未支付调用，关单接口，更新本地订单状态
            this.closeOrder(orderNo);
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, String reason) {
        log.info("支付宝退款API");
        try {
            RefundInfo refundInfo = refundInfoService.createRefundInfoByOrderNoForAliPay(orderNo, reason);

            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            BigDecimal refund = new BigDecimal(refundInfo.getRefund()).divide(new BigDecimal("100"));
            bizContent.put("refund_amount", refund);
            bizContent.put("refund_reason", reason);

            request.setBizContent(bizContent.toString());

            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("退款成功，返回结果：{}", response.getBody());
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_SUCCESS);
                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliTradeState.REFUND_SUCESS.getType());
            } else {
                log.info("退款失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_ABNORMAL);
                refundInfoService.updateRefundForAliPay(
                        refundInfo.getRefundNo(),
                        response.getBody(),
                        AliTradeState.REFUND_ERROR.getType());
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String queryRefund(String orderNo) {

        log.info("退款接口调用，订单号：{}", orderNo);

        try {
            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("out_request_no", orderNo);

            request.setBizContent(bizContent.toString());

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("退款成功，返回结果：{}", response.getBody());
                return response.getBody();
            } else {
                log.info("退款失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
                return null;
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("退款查询接口异常");
        }
    }

    @Override
    public String queryBill(String billDate, String type) {
        try {
            AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("bill_type", type);
            bizContent.put("bill_date", billDate);
            request.setBizContent(bizContent.toString());

            AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                log.info("申请账单成功，返回结果：{}", response.getBody());
                Gson gson = new Gson();
                Map<String, LinkedTreeMap> resultMap = gson.fromJson(response.getBody(), HashMap.class);
                LinkedTreeMap billDownloadurlResponse = resultMap.get("alipay_data_dataservice_bill_downloadurl_query_response");
                return (String) billDownloadurlResponse.get("bill_download_url");
            } else {
                log.info("申请账单失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
                throw new RuntimeException("申请账单失败");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("申请账单失败");
        }
    }

    /**
     * 关单接口调用
     *
     * @param orderNo:
     * @return void
     * @author: Gary
     * @date: 2022/9/6 下午4:36
     */
    private void closeOrder(String orderNo) {

        log.info("关单接口调用，订单号：{}", orderNo);
        try {
            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
            JSONObject bizContent = new JSONObject();
            bizContent.put("out_trade_no", orderNo);
            request.setBizContent(bizContent.toString());
            AlipayTradeCloseResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("调用支付宝成功，返回结果：{}", response.getBody());
            } else {
                log.info("调用支付宝失败，返回码：{}，返回描述：{}", response.getCode(), response.getMsg());
//                throw new RuntimeException("关单接口调用失败");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new RuntimeException("关单接口调用失败");
        }


    }
}
