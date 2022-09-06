package com.javabgy.paymentdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.enums.OrderStatus;

import java.util.List;

/**
 * 订单表Service
 *
 * @author Gary
 * @date 2022/9/2 下午11:02
 **/
public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 根据productId生成订单
     * @param productId:
     * @param paymentType:
     * @return com.javabgy.paymentdemo.entity.OrderInfo
     * @author: Gary
     * @date: 2022/9/4 上午10:33
     */
    OrderInfo createOrderByProductId(Long productId, String paymentType);

    /**
     * 保存订单codeUrl
     * @param orderNo:
     * @param codeUrl:
     * @return void
     * @author: Gary
     * @date: 2022/9/4 下午4:25
     */
    void saveOrderCodeUrl(String orderNo, String codeUrl);

    /**
     * 订单列表
     * @param :
     * @return java.util.List<com.javabgy.paymentdemo.entity.OrderInfo>
     * @author: Gary
     * @date: 2022/9/4 下午5:07
     */
    List<OrderInfo> listByCreateTimeDesc();

    void updateStatusByOrderNo(String orderNo, OrderStatus status);

    String getOrderStatus(String orderNo);

    List<OrderInfo> getNoPayOrderByDuration(int minutes, String paymentType);

    OrderInfo getOrderByOrderNo(String orderNo);
}
