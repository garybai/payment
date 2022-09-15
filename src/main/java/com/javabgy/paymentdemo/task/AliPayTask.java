package com.javabgy.paymentdemo.task;

import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.enums.PayType;
import com.javabgy.paymentdemo.service.AliPayService;
import com.javabgy.paymentdemo.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 支付宝定时
 *
 * @author Gary
 * @date 2022/9/6 下午5:08
 **/
@Slf4j
@Component
public class AliPayTask {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private AliPayService aliPayService;

    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() {
        log.info("支付宝orderConfirm执行");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5, PayType.ALIPAY.getType());
        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.warn("超时订单orderNo：{}", orderInfo);

            // 核实订单状态
            aliPayService.checkOrderStatus(orderNo);
        }
    }

}
