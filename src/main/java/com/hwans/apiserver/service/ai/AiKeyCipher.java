package com.hwans.apiserver.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 회원의 Anthropic API 키를 AES-256-GCM 으로 암호화·복호화한다.
 * <p>
 * 마스터 키는 {@code ai.encryption-key}(base64 로 쓴 32바이트, 운영은 VM 의 application-secret.yml 또는 환경변수
 * {@code AI_ENCRYPTION_KEY})이다. 없거나 잘못됐으면 AI 기능 전체가 꺼진다(서버는 정상 기동).
 * 저장 형식은 {@code v1:} + base64(IV 12바이트 + 암호문 + 태그 16바이트).
 */
@Component
@Slf4j
public class AiKeyCipher {
    private static final String VERSION_PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public AiKeyCipher(@Value("${ai.encryption-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    private static SecretKey parseKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            log.info("ai.encryption-key is not set: AI writing features are disabled");
            return null;
        }
        try {
            var bytes = Base64.getDecoder().decode(base64Key.trim());
            if (bytes.length != 32) {
                log.error("ai.encryption-key must be 32 bytes (base64), got {} bytes: AI writing features are disabled", bytes.length);
                return null;
            }
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException e) {
            log.error("ai.encryption-key is not valid base64: AI writing features are disabled");
            return null;
        }
    }

    /**
     * 마스터 키가 있어 AI 기능을 쓸 수 있는지
     */
    public boolean isEnabled() {
        return key != null;
    }

    public String encrypt(String plainText) {
        requireEnabled();
        try {
            var iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            var encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            var out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to encrypt API key", e);
        }
    }

    public String decrypt(String stored) {
        requireEnabled();
        if (stored == null || !stored.startsWith(VERSION_PREFIX)) {
            throw new IllegalStateException("unknown encrypted API key format");
        }
        try {
            var data = Base64.getDecoder().decode(stored.substring(VERSION_PREFIX.length()));
            if (data.length <= IV_BYTES) {
                throw new IllegalStateException("encrypted API key is too short");
            }
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            var plain = cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // 마스터 키가 바뀌었거나 값이 손상됨. 원인 메시지에 키 정보는 없다.
            throw new IllegalStateException("failed to decrypt API key", e);
        }
    }

    private void requireEnabled() {
        if (key == null) {
            throw new IllegalStateException("ai.encryption-key is not configured");
        }
    }
}
