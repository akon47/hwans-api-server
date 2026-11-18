package com.hwans.apiserver.service.ai;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AiKeyCipherTest {
    private static final String MASTER_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void roundTrip() {
        var cipher = new AiKeyCipher(MASTER_KEY);
        assertTrue(cipher.isEnabled());
        var apiKey = "sk-ant-api03-한글도-되는지-AbCd";
        var encrypted = cipher.encrypt(apiKey);
        assertTrue(encrypted.startsWith("v1:"));
        assertFalse(encrypted.contains(apiKey));
        assertEquals(apiKey, cipher.decrypt(encrypted));
    }

    @Test
    void randomIvGivesDifferentCiphertexts() {
        var cipher = new AiKeyCipher(MASTER_KEY);
        var a = cipher.encrypt("sk-ant-same");
        var b = cipher.encrypt("sk-ant-same");
        assertNotEquals(a, b);
        assertEquals("sk-ant-same", cipher.decrypt(a));
        assertEquals("sk-ant-same", cipher.decrypt(b));
        // IV 12 + 평문 11 + 태그 16
        assertEquals(12 + 11 + 16, Base64.getDecoder().decode(a.substring(3)).length);
    }

    @Test
    void tamperedCiphertextIsRejected() {
        var cipher = new AiKeyCipher(MASTER_KEY);
        var bytes = Base64.getDecoder().decode(cipher.encrypt("sk-ant-secret").substring(3));
        bytes[bytes.length - 1] ^= 1;
        var tampered = "v1:" + Base64.getEncoder().encodeToString(bytes);
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void otherMasterKeyCannotDecrypt() {
        var encrypted = new AiKeyCipher(MASTER_KEY).encrypt("sk-ant-secret");
        assertThrows(IllegalStateException.class, () -> new AiKeyCipher(OTHER_KEY).decrypt(encrypted));
    }

    @Test
    void missingOrInvalidMasterKeyDisablesFeature() {
        for (var key : new String[]{null, "", "   ", "not base64 !!", Base64.getEncoder().encodeToString(new byte[16])}) {
            var cipher = new AiKeyCipher(key);
            assertFalse(cipher.isEnabled(), "key: " + key);
            assertThrows(IllegalStateException.class, () -> cipher.encrypt("x"));
        }
    }
}
