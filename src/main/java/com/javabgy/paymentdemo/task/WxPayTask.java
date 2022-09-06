package com.javabgy.paymentdemo.task;

import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.enums.PayType;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 定时任务
 *
 * @author Gary
 * @date 2022/9/5 上午11:27
 **/
@Component
@Slf4j
public class WxPayTask {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    /**
     * 秒 分 时 日 月 周
     *
     * *：每秒都执行
     * ?：不指定
     * 1-3：从第1秒开始执行，到第3秒结束执行
     * 0/3：从第0秒开始执行，每隔3秒执行一次
     * 1,2,3：指定第1秒，第2秒，第3秒执行
     *
     * 日和周互斥，指定一个，另一个设置为?
     */
//    @Scheduled(cron = "1-3 * * * * ?")
//    public void task1() {
//        log.info("task1 执行");
//    }

//    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws Exception {

        log.info("微信orderConfirm执行");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderByDuration(5, PayType.WXPAY.getType());
        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.warn("超时订单orderNo：{}", orderInfo);

            // 核实订单状态
            wxPayService.checkOrderStatus(orderNo);

        }

    }

}
