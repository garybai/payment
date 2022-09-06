package com.javabgy.paymentdemo.config;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import com.wechat.pay.contrib.apache.httpclient.exception.HttpCodeException;
import com.wechat.pay.contrib.apache.httpclient.exception.NotFoundException;
import com.wechat.pay.contrib.apache.httpclient.util.PemUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * 微信支付配置文件
 *
 * @author Gary
 * @date 2022/9/3 下午3:51
 **/
@Configuration
@PropertySource("classpath:wxpay.properties")
@ConfigurationProperties(prefix = "wxpay")
@Data
@Slf4j
public class WxPayConfig {

    private String mchId;

    private String mchSerialNo;

    private String privateKeyPath;

    private String apiV3Key;

    private String appid;

    private String domain;

    private String notifyDomain;

    /**
     * 获取商户的私钥文件
     *
     * @param filename
     * @return
     */
    private PrivateKey getPrivateKey(String filename) {

        try {
            return PemUtil.loadPrivateKey(new FileInputStream(filename));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("私钥文件不存在", e);
        }
    }

    /**
     * 获取签名验证器
     *
     * @param :
     * @return com.wechat.pay.contrib.apache.httpclient.auth.Verifier
     * @author: Gary
     * @date: 2022/9/3 下午5:07
     */
    @Bean
    public Verifier getVerifier() throws NotFoundException, HttpCodeException, GeneralSecurityException, IOException {

        log.info("获取签名验证器");
        // 获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        // 获取证书管理器实例
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        // 向证书管理器增加需要自动更新平台证书的商户信息
        // 获取私钥签名对象
        PrivateKeySigner signer = new PrivateKeySigner(mchSerialNo, privateKey);
        // 身份认证对象
        WechatPay2Credentials credentials = new WechatPay2Credentials(mchId, signer);
        certificatesManager.putMerchant(mchId, credentials, apiV3Key.getBytes(StandardCharsets.UTF_8));
        // ... 若有多个商户号，可继续调用putMerchant添加商户信息

        // 从证书管理器中获取verifier
        return certificatesManager.getVerifier(mchId);
    }

    /**
     * 获取http请求对象
     *
     * @param verifier:
     * @return org.apache.http.impl.client.CloseableHttpClient
     * @author: Gary
     * @date: 2022/9/3 下午5:07
     */
    @Bean(name = "wxPayClient")
    public CloseableHttpClient getWxPayClient(Verifier verifier) {
        log.info("获取http请求对象");
        PrivateKey privateKey = getPrivateKey(privateKeyPath);
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                .withMerchant(mchId, mchSerialNo, privateKey)
                .withValidator(new WechatPay2Validator(verifier));
        // ... 接下来，你仍然可以通过builder设置各种参数，来配置你的HttpClient

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        return builder.build();

        // 后面跟使用Apache HttpClient一样
        // CloseableHttpResponse response = httpClient.execute(...);
    }

    @Bean(name = "wxPayNoSignClient")
    public CloseableHttpClient getWxPayNoSignClient(){

        //获取商户私钥
        PrivateKey privateKey = getPrivateKey(privateKeyPath);

        //用于构造HttpClient
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create()
                //设置商户信息
                .withMerchant(mchId, mchSerialNo, privateKey)
                //无需进行签名验证、通过withValidator((response) -> true)实现
                .withValidator((response) -> true);

        // 通过WechatPayHttpClientBuilder构造的HttpClient，会自动的处理签名和验签，并进行证书自动更新
        CloseableHttpClient httpClient = builder.build();

        log.info("== getWxPayNoSignClient END ==");

        return httpClient;
    }

}
