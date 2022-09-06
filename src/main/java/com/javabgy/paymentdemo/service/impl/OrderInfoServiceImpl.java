package com.javabgy.paymentdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.entity.Product;
import com.javabgy.paymentdemo.enums.OrderStatus;
import com.javabgy.paymentdemo.mapper.OrderInfoMapper;
import com.javabgy.paymentdemo.mapper.ProductMapper;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.util.OrderNoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * OrderInfoService实现类
 *
 * @author Gary
 * @date 2022/9/2 下午11:04
 **/
@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
        implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderInfoMapper orderInfoMapper;

    /**
     * 根据productId生成订单
     *
     * @param productId :
     * @return com.javabgy.paymentdemo.entity.OrderInfo
     * @author: Gary
     * @date: 2022/9/4 上午10:33
     */
    @Override
    public OrderInfo createOrderByProductId(Long productId, String paymentType) {

        // 查询已有未支付订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId, paymentType);
        if (orderInfo != null) {
            return orderInfo;
        }

        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // 生成订单
        orderInfo = new OrderInfo();
        orderInfo.setProductId(productId);
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);

        orderInfoMapper.insert(orderInfo);

        return orderInfo;
    }

    /**
     * 保存订单codeUrl
     *
     * @param orderNo :
     * @param codeUrl :
     * @return void
     * @author: Gary
     * @date: 2022/9/4 下午4:25
     */
    @Override
    public void saveOrderCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);
        baseMapper.update(orderInfo, queryWrapper);
    }

    /**
     * 订单列表
     *
     * @return java.util.List<com.javabgy.paymentdemo.entity.OrderInfo>
     * @author: Gary
     * @date: 2022/9/4 下午5:07
     */
    @Override
    public List<OrderInfo> listByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");

        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 根据订单号更新订单状态
     * @param orderNo:
     * @param status:
     * @return void
     * @author: Gary
     * @date: 2022/9/4 下午8:56
     */
    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus status) {
        log.info("更新订单状态：{}", status.getType());
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(status.getType());
        baseMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public String getOrderStatus(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);
        if (orderInfo == null) {
            return null;
        }
        return orderInfo.getOrderStatus();
    }

    /**
     * 查询超过minutes未支付的订单
     * @param minutes:
     * @return java.util.List<com.javabgy.paymentdemo.entity.OrderInfo>
     * @author: Gary
     * @date: 2022/9/5 上午11:42
     */
    @Override
    public List<OrderInfo> getNoPayOrderByDuration(int minutes, String paymentType) {
        Instant instant = Instant.now().minus(Duration.ofMinutes(minutes));
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.le("create_time", instant);
        queryWrapper.eq("payment_type", paymentType);

        List<OrderInfo> orderInfoList = baseMapper.selectList(queryWrapper);
        return orderInfoList;
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 根据商品id查询未支付订单，防止重复创建
     * @param productId:
     * @return com.javabgy.paymentdemo.entity.OrderInfo
     * @author: Gary
     * @date: 2022/9/4 上午10:56
     */
    private OrderInfo getNoPayOrderByProductId(Long productId, String paymentType) {

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.eq("payment_type", paymentType);
        OrderInfo orderInfo = baseMapper.selectOne(queryWrapper);

        return orderInfo;
    }
}
