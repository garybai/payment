package com.javabgy.paymentdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.javabgy.paymentdemo.entity.PaymentInfo;
import com.javabgy.paymentdemo.enums.PayType;
import com.javabgy.paymentdemo.mapper.PaymentInfoMapper;
import com.javabgy.paymentdemo.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gary
 * @date 2022/9/4 下午8:53
 **/
@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo>
        implements PaymentInfoService {

    @Override
    public void createPaymentInfo(String plainText) {
        log.info("记录支付日志");
        Gson gson = new Gson();
        HashMap plainTextMap = gson.fromJson(plainText, HashMap.class);

        String orderNo = (String) plainTextMap.get("out_trade_no");
        String transactionId = (String) plainTextMap.get("transaction_id");
        String tradeType = (String) plainTextMap.get("trade_type");
        String tradeState = (String) plainTextMap.get("trade_state");
        Map<String, Object> amountMap = (Map) plainTextMap.get("amount");
        int payerTotal = ((Double) amountMap.get("payer_total")).intValue();


        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);

    }

    @Override
    public void createPaymentInfoForAliPay(Map<String, String> params) {
        log.info("记录支付日志");

        String outTradeNo = params.get("out_trade_no");
        String transactionId = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");
        int totalAmountInt = new BigDecimal(totalAmount).multiply(new BigDecimal("100")).intValue();
        Gson gson = new Gson();
        String content = gson.toJson(params, HashMap.class);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(outTradeNo);
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setPaymentType(PayType.ALIPAY.getType());
        paymentInfo.setTradeType("电脑网站支付");
        paymentInfo.setTradeState(tradeStatus);
        paymentInfo.setPayerTotal(totalAmountInt);
        paymentInfo.setContent(content);

        baseMapper.insert(paymentInfo);
    }
}
