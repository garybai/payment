package com.javabgy.paymentdemo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 产品表
 *
 * @author Gary
 * @date 2022/9/2 下午11:11
 **/
@Data
@TableName("t_product")
public class Product extends BaseEntity {

    private String title;

    private Integer price;

}
