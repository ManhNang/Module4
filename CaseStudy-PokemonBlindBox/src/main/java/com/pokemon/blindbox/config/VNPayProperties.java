package com.pokemon.blindbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VNPayProperties {
    private String tmnCode = "DEMO_TMN_CODE";
    private String hashSecret = "DEMO_VNPAY_HMAC_SHA512_SECRET_KEY_2026";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:5173/wallet/vnpay-return";
    private String version = "2.1.0";
    private String command = "pay";
    private String currCode = "VND";
    private String orderType = "topup";
    private int expireMinutes = 15;
}
