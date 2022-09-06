package com.javabgy.paymentdemo.controller;

import com.google.gson.Gson;
import com.javabgy.paymentdemo.service.WxPayService;
import com.javabgy.paymentdemo.util.HttpUtils;
import com.javabgy.paymentdemo.util.WechatPay2ValidatorForRequest;
import com.javabgy.paymentdemo.vo.R;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付控制器
 *
 * @author Gary
 * @date 2022/9/3 下午5:59
 **/
@CrossOrigin
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付API")
@Slf4j
public class WxPayController {

    @Autowired
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;

    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("/native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");
        // 返回支付二维码连接和订单号
        Map<String, Object> map = wxPayService.nativePay(productId);
        return R.ok().setData(map);
    }

    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new Gson();
        try {
            // 处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知id：{}", requestId);
//        log.info("支付通知完整内容：{}", body);

            // 签名验证
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                // 失败应答
                Map<String, String> map = new HashMap<>();
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                response.setStatus(500);
                log.error("通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");
            // 处理订单
            wxPayService.processOrder(bodyMap);

            // 成功应答
            Map<String, String> map = new HashMap<>();
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            response.setStatus(200);
            return gson.toJson(map);
        } catch (Exception e) {
            // 失败应答
            Map<String, String> map = new HashMap<>();
            map.put("code", "ERROR");
            map.put("message", "失败");
            response.setStatus(500);
            return gson.toJson(map);
        }
    }

    @PostMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable String orderNo) throws Exception {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("查询订单");
        String result = wxPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws Exception {
        log.info("申请退款");
        wxPayService.refund(orderNo, reason);
        return R.ok();
    }

    @GetMapping("/query-refund/{refundNo}")
    @ApiOperation("查询退款，测试")
    public R queryRefund(@PathVariable String refundNo) throws Exception {
        log.info("查询退款");
        String result = wxPayService.queryRefund(refundNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    @PostMapping("/refunds/notify")
    public String refundNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("退款通知执行");
        Gson gson = new Gson();
        try {
            // 处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("退款通知id：{}", requestId);
//        log.info("支付通知完整内容：{}", body);

            // 签名验证
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!validator.validate(request)) {
                // 失败应答
                Map<String, String> map = new HashMap<>();
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                response.setStatus(500);
                log.error("通知验签失败");
                return gson.toJson(map);
            }
            log.info("通知验签成功");
            // 处理退款单
            wxPayService.processRefund(bodyMap);

            // 成功应答
            Map<String, String> map = new HashMap<>();
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            response.setStatus(200);
            return gson.toJson(map);
        } catch (Exception e) {
            // 失败应答
            Map<String, String> map = new HashMap<>();
            map.put("code", "ERROR");
            map.put("message", "失败");
            response.setStatus(500);
            return gson.toJson(map);
        }
    }

    @GetMapping("/querybill/{billDate}/{type}")
    @ApiOperation("获取账单url测试")
    public R queryBill(@PathVariable String billDate, @PathVariable String type) throws Exception {
        log.info("获取账单url");
        String downloadUrl = wxPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取url成功").data("downloadUrl", downloadUrl);
    }

    @GetMapping("/downloadbill/{billDate}/{type}")
    @ApiOperation("下载账单")
    public R downloadBill(@PathVariable String billDate, @PathVariable String type) throws Exception {
        log.info("下载账单");
        String result = wxPayService.downloadBill(billDate, type);
        return R.ok().data("result", result);
    }
}
