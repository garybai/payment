package com.javabgy.paymentdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javabgy.paymentdemo.entity.PaymentInfo;

import java.util.Map;

/**
 * @author Gary
 * @date 2022/9/4 下午8:53
 **/
public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 记录支付日志
     * @param plainText:
     * @return void
     * @author: Gary
     * @date: 2022/9/4 下午9:06
     */
    void createPaymentInfo(String plainText);

    /**
     * 记录支付日志：Alipay
     * @param params:
     * @return void
     * @author: Gary
     * @date: 2022/9/6 下午3:50
     */
    void createPaymentInfoForAliPay(Map<String, String> params);
}
