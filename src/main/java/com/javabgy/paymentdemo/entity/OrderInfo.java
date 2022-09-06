package com.javabgy.paymentdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单表
 *
 * @author Gary
 * @date 2022/9/2 下午10:56
 **/
@Data
@TableName("t_order_info")
public class OrderInfo extends BaseEntity {

    private String title;

    private String orderNo;

    private Long userId;

    private Long productId;

    private Integer totalFee;

    private String codeUrl;

    private String orderStatus;

    private String paymentType;

}
