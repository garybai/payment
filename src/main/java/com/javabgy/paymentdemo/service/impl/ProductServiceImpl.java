package com.javabgy.paymentdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javabgy.paymentdemo.entity.Product;
import com.javabgy.paymentdemo.mapper.ProductMapper;
import com.javabgy.paymentdemo.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * 商品表Service实现
 *
 * @author Gary
 * @date 2022/9/2 下午11:14
 **/
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
        implements ProductService {
}
