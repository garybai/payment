package com.javabgy.paymentdemo.controller;

import com.javabgy.paymentdemo.config.WxPayConfig;
import com.javabgy.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 测试控制器
 *
 * @author Gary
 * @date 2022/9/3 下午4:00
 **/
@Api(tags = "测试控制器")
@RequestMapping("/api/test")
@RestController
public class TestController {

    @Resource
    private WxPayConfig wxPayConfig;

    @GetMapping
    @ApiOperation("测试接口")
    public R test() {
        String appid = wxPayConfig.getAppid();
        return R.ok().data("appid", appid);
    }

}
