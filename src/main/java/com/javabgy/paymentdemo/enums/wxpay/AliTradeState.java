package com.javabgy.paymentdemo.enums.wxpay;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AliTradeState {

    /**
     * 支付成功
     */
    SUCCESS("TRADE_SUCCESS"),

    /**
     * 未支付
     */
    NOTPAY("WAIT_BUYER_PAY"),

    /**
     * 已关闭
     */
    CLOSED("TRADE_CLOSED"),

    /**
     * 转入退款
     */
    REFUND_SUCESS("REFUND_SUCESS"),
    REFUND_ERROR("REFUND_ERROR");

    /**
     * 类型
     */
    private final String type;
}
