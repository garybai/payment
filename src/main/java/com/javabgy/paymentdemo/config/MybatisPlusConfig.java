package com.javabgy.paymentdemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MybatisPlus配置
 *
 * @author Gary
 * @date 2022/9/2 下午11:07
 **/
@Configuration
@EnableTransactionManagement
@MapperScan("com.javabgy.paymentdemo.mapper")
public class MybatisPlusConfig {

}
