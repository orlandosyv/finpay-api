package com.finpay.api.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookSecretCrypto {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookSecretCrypto.class);
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec encryptionKey;

    public WebhookSecretCrypto(
            @Value("${finpay.webhook.encryption-key:${FINPAY_JWT_SECRET:}}")
            String configuredKey) {
        String keyMaterial = configuredKey;
        if (keyMaterial == null || keyMaterial.isBlank()) {
            byte[] ephemeralKey = new byte[32];
            secureRandom.nextBytes(ephemeralKey);
            keyMaterial = Base64.getEncoder().encodeToString(ephemeralKey);
            LOGGER.warn("FINPAY_WEBHOOK_ENCRYPTION_KEY is not configured. Using an ephemeral development key; stored webhook secrets cannot be decrypted after restart.");
        }
        this.encryptionKey = new SecretKeySpec(sha256(keyMaterial), "AES");
    }

    public String generateSigningSecret() {
        byte[] secret = new byte[32];
        secureRandom.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer
                    .allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Webhook secret could not be encrypted", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Webhook secret could not be decrypted", exception);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
