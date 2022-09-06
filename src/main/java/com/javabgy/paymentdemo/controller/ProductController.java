package com.javabgy.paymentdemo.controller;

import com.javabgy.paymentdemo.entity.Product;
import com.javabgy.paymentdemo.service.ProductService;
import com.javabgy.paymentdemo.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 商品接口
 *
 * @author Gary
 * @date 2022/9/2 下午9:55
 **/
@CrossOrigin
@RestController
@RequestMapping("/api/product")
@Api(tags = "商品管理")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/test")
    @ApiOperation("测试接口")
    public R test() {
        return R.ok().data("message", "hello").data("now", new Date());
    }

    @GetMapping("list")
    @ApiOperation("商品列表")
    public R list() {
        List<Product> list = productService.list();
        return R.ok().data("productList", list);
    }
}
