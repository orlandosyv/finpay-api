package com.finpay.api.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class WebhookSignatureService {

    public String sign(String signingSecret, String timestamp, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    signingSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] signature = mac.doFinal(
                    (timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            return "v1=" + HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Webhook signature could not be generated", exception);
        }
    }
}
