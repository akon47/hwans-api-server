package com.hwans.apiserver.service.ai.cover;

import lombok.Getter;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 커버 이미지 배경 테마 (왼쪽 위 → 오른쪽 아래 그라데이션)
 */
@Getter
public enum CoverTheme {
    SECURITY_RED("security-red", 0x450A0A, 0xB91C1C, 0xF87171),
    OCEAN_BLUE("ocean-blue", 0x082F49, 0x0369A1, 0x38BDF8),
    FOREST_GREEN("forest-green", 0x052E16, 0x15803D, 0x4ADE80),
    SUNSET_ORANGE("sunset-orange", 0x431407, 0xC2410C, 0xFB923C),
    VIOLET("violet", 0x2E1065, 0x6D28D9, 0xA78BFA),
    SLATE("slate", 0x020617, 0x334155, 0x94A3B8);

    private final String key;
    private final Color from;
    private final Color to;
    private final Color accent;

    CoverTheme(String key, int from, int to, int accent) {
        this.key = key;
        this.from = new Color(from);
        this.to = new Color(to);
        this.accent = new Color(accent);
    }

    public static Optional<CoverTheme> fromKey(String key) {
        return Arrays.stream(values()).filter(x -> x.key.equals(key)).findFirst();
    }

    public static List<String> keys() {
        return Arrays.stream(values()).map(CoverTheme::getKey).toList();
    }
}
