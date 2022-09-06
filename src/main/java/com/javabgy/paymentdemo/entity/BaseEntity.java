package com.javabgy.paymentdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * BaseEntity
 *
 * @author Gary
 * @date 2022/9/2 下午10:53
 **/
@Data
public class BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private String id;
    private Date createTime;
    private Date updateTime;

}
