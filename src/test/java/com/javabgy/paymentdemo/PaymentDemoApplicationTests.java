package com.javabgy.paymentdemo;

import com.javabgy.paymentdemo.config.WxPayConfig;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class PaymentDemoApplicationTests {

    @Resource
    private WxPayConfig wxPayConfig;

//    @Test
//    void testGetPrivateKey() {
//        String privateKeyPath = wxPayConfig.getPrivateKeyPath();
//        PrivateKey privateKey = wxPayConfig.getPrivateKey(privateKeyPath);
//        System.out.println(privateKey);
//    }

}
