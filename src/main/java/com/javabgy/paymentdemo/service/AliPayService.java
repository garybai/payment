package com.javabgy.paymentdemo.service;

import java.util.Map;

/**
 * alipay
 *
 * @author Gary
 * @date 2022/9/5 下午10:39
 **/
public interface AliPayService {
    String tradeCreate(Long productId);

    void processOrder(Map<String, String> params);

    void cancleOrder(String orderNo);

    String queryOrder(String orderNo);

    void checkOrderStatus(String orderNo);

    void refund(String orderNo, String reason);

    String queryRefund(String orderNo);

    String queryBill(String billDate, String type);
}
