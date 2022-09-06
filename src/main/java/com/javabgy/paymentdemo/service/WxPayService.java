package com.javabgy.paymentdemo.service;

import java.util.Map;

/**
 * 微信支付
 *
 * @author Gary
 * @date 2022/9/3 下午6:01
 **/
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;

    void processOrder(Map<String, Object> bodyMap) throws Exception;

    void cancelOrder(String orderNo) throws Exception;

    String queryOrder(String orderNo) throws Exception;

    void checkOrderStatus(String orderNo) throws Exception;

    void refund(String orderNo, String reason) throws Exception;

    String queryRefund(String refundNo) throws Exception;

    void processRefund(Map<String, Object> bodyMap) throws Exception;

    String queryBill(String billDate, String type) throws Exception;

    String downloadBill(String billDate, String type) throws Exception;
}
