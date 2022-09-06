package com.javabgy.paymentdemo.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.service.AliPayService;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝控制器
 *
 * @author Gary
 * @date 2022/9/5 下午10:34
 **/
@CrossOrigin
@RestController
@RequestMapping("/api/ali-pay")
@Api(tags = "支付宝网站支付")
@Slf4j
public class AliPayController {

    @Resource
    private AliPayService aliPayService;

    @Resource
    private Environment config;

    @Resource
    private OrderInfoService orderInfoService;

    @PostMapping("/trade/page/pay/{productId}")
    @ApiOperation("统一收单下单并支付页面接口")
    public R tradePagePau(@PathVariable Long productId) {
        log.info("统一收单下单并支付页面接口");

        // 支付宝开放平台接收request请求对象后
        // 会为开发者生成一个html形式的表单，包含自动提交的脚本
        String formStr = aliPayService.tradeCreate(productId);

        // 将form表单字符串返回给前端，之后前端自动调用脚本提交
        // 表单自动提交到action指向的支付宝开放平台接口，从而为用户展示支付页面
        return R.ok().data("formStr", formStr);
    }

    @PostMapping("/trade/notify")
    @ApiOperation("支付宝支付通知")
    public String tradeNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝支付通知执行");
        log.info("通知参数：{}", params);

        String result = "failure";

        // 异步通知验签
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    config.getProperty("alipay.alipay-public-key"),
                    AlipayConstants.CHARSET_UTF8,
                    AlipayConstants.SIGN_TYPE_RSA2);
            if(!signVerified){
                log.error("支付宝异步通知验签失败");
                return result;
            }

            log.info("支付宝异步通知验签成功");
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
            // 1.商家需要验证该通知数据中的 out_trade_no 是否为商家系统中创建的订单号。
            String outTradeNo = params.get("out_trade_no");
            OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(outTradeNo);
            if (orderInfo == null) {
                log.error("订单不存在");
                return result;
            }
            // 2.判断 total_amount 是否确实为该订单的实际金额（即商家订单创建时的金额）。
            String totalAmount = params.get("total_amount");
            int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();
            int totalFeeInt = orderInfo.getTotalFee();
            if (totalAmountInt != totalFeeInt) {
                log.error("订单晋哥校验失败");
                return result;
            }
            // 3.校验通知中的 seller_id（或者 seller_email) 是否为 out_trade_no 这笔单据的对应的操作方（有的时候，一个商家可能有多个 seller_id/seller_email）。
            String sellerId = params.get("seller_id");
            String sellerId1 = config.getProperty("alipay.seller-id");
            if (!sellerId.equals(sellerId1)) {
                log.error("商家pid校验失败");
                return result;
            }
            // 4.验证 app_id 是否为该商家本身。
            String appId = params.get("app_id");
            String appId1 = config.getProperty("alipay.app-id");
            if (!appId.equals(appId1)) {
                log.error("appId校验失败");
                return result;
            }

            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus)) {
                log.error("支付未成功");
                return result;
            }
            // 校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 处理业务逻辑 修改订单状态 记录支付日志
            aliPayService.processOrder(params);

            result = "success";
        } catch (AlipayApiException e) {
            e.printStackTrace();
            log.error("支付宝异步通知验签异常");
        }
        return result;
    }

    /**
     * 用户取消订单
     * @param orderNo:
     * @return com.javabgy.paymentdemo.vo.R
     * @author: Gary
     * @date: 2022/9/6 下午4:33
     */
    @PostMapping("/trade/close/{orderNo}")
    @ApiOperation("用户取消订单")
    public R cancle(@PathVariable String orderNo) {
        log.info("取消订单：{}", orderNo);
        aliPayService.cancleOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    /**
     * 查询订单
     * @param orderNo:
     * @return com.javabgy.paymentdemo.vo.R
     * @author: Gary
     * @date: 2022/9/6 下午4:50
     */
    @GetMapping("/trade/query/{orderNo}")
    @ApiOperation("查询订单")
    public R queryOrder(@PathVariable String orderNo) {
        log.info("查询订单");
        String result = aliPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    /**
     * 用户申请退款
     * @param orderNo:
     * @param reason:
     * @return com.javabgy.paymentdemo.vo.R
     * @author: Gary
     * @date: 2022/9/6 下午6:16
     */
    @PostMapping("/trade/refund/{orderNo}/{reason}")
    @ApiOperation("用户申请退款")
    public R refund(@PathVariable String orderNo, @PathVariable String reason) {
        log.info("申请退款：{}", orderNo);
        aliPayService.refund(orderNo, reason);
        return R.ok();
    }

    /**
     * 查询退款
     * @param orderNo:
     * @return com.javabgy.paymentdemo.vo.R
     * @author: Gary
     * @date: 2022/9/6 下午6:51
     */
    @GetMapping("/trade/fastpay/refund/{orderNo}")
    @ApiOperation("查询退款")
    public R queryRefund(@PathVariable String orderNo) {
        log.info("查询退款");
        String result = aliPayService.queryRefund(orderNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    @GetMapping("/bill/downloadurl/query/{billDate}/{type}")
    @ApiOperation("获取账单url测试")
    public R queryBill(@PathVariable String billDate, @PathVariable String type) throws Exception {
        log.info("获取账单url");
        String downloadUrl = aliPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取url成功").data("downloadUrl", downloadUrl);
    }

}
