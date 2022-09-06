package com.javabgy.paymentdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.javabgy.paymentdemo.entity.OrderInfo;
import com.javabgy.paymentdemo.entity.RefundInfo;
import com.javabgy.paymentdemo.mapper.RefundInfoMapper;
import com.javabgy.paymentdemo.service.OrderInfoService;
import com.javabgy.paymentdemo.service.RefundInfoService;
import com.javabgy.paymentdemo.util.OrderNoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 退款Service实现
 *
 * @author Gary
 * @date 2022/9/5 下午3:43
 **/
@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo>
        implements RefundInfoService {

    @Resource
    private OrderInfoService orderInfoService;

    @Override
    public RefundInfo createRefundInfoByOrderNo(String orderNo, String reason) {
        // 根据订单编号获取订单信息
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        baseMapper.insert(refundInfo);
        return refundInfo;
    }

    @Override
    public void updateRefund(String bodyAsString) {
        Gson gson = new Gson();
        Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);

        // 根据退款单号修改退款单
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", resultMap.get("out_refund_no"));

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundId(resultMap.get("refund_id"));

        if (resultMap.get("status") != null) {
            refundInfo.setRefundStatus(resultMap.get("status"));
            refundInfo.setContentReturn(bodyAsString);
        }
        if (resultMap.get("refund_status") != null) {
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            refundInfo.setContentNotify(bodyAsString);
        }
        baseMapper.update(refundInfo, queryWrapper);
    }

    @Override
    public RefundInfo createRefundInfoByOrderNoForAliPay(String orderNo, String reason) {
        OrderInfo orderInfo = orderInfoService.getOrderByOrderNo(orderNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        refundInfo.setRefund(orderInfo.getTotalFee());
        refundInfo.setReason(reason);

        baseMapper.insert(refundInfo);
        return refundInfo;
    }

    @Override
    public void updateRefundForAliPay(String refundNo, String body, String refundStatus) {
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", refundNo);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundStatus(refundStatus);
        refundInfo.setContentReturn(body);

        baseMapper.update(refundInfo, queryWrapper);
    }
}
