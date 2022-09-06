package com.javabgy.paymentdemo.controller;

import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.enums.OrderStatus;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单管理
 *
 * @author Gary
 * @date 2022/9/4 下午5:04
 **/
@CrossOrigin
@RestController
@RequestMapping("/api/order-info")
@Api(tags = "订单管理")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @GetMapping("/list")
    public R list() {
        List<OrderInfo> list = orderInfoService.listByCreateTimeDesc();
        return R.ok().data("list", list);
    }

    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo) {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            return R.ok().setMessage("支付成功");
        }
        return R.ok().setCode(101).setMessage("支付中......");
    }

}
