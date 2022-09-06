package com.javabgy.paymentdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javabgy.paymentdemo.entity.RefundInfo;

/**
 * 退款接口
 *
 * @author Gary
 * @date 2022/9/5 下午3:42
 **/
public interface RefundInfoService extends IService<RefundInfo> {
    RefundInfo createRefundInfoByOrderNo(String orderNo, String reason);

    void updateRefund(String bodyAsString);

    RefundInfo createRefundInfoByOrderNoForAliPay(String orderNo, String reason);

    void updateRefundForAliPay(String refundNo, String body, String refundStatus);
}
