package com.hwans.apiserver.service.ai.cover;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CoverRendererTest {
    private final CoverRenderer renderer = new CoverRenderer();

    @Test
    void rendersA1200x630Png() throws Exception {
        var design = CoverDesign.of("CVE-2026-114938 자바 보안 취약점", "어떤 문제이고 어떻게 고치는지 정리했어요",
                "CVE-2026-114938", "security-red", "shield");
        var png = renderer.renderPng(design, "hwanstory.kr/@akon47");

        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(CoverRenderer.WIDTH, image.getWidth());
        assertEquals(CoverRenderer.HEIGHT, image.getHeight());
        assertEquals(1200, image.getWidth());
        assertEquals(630, image.getHeight());

        // 비밀값이 아닌 경로: 운영자가 눈으로 볼 샘플 (COVER_SAMPLE_PATH 가 있을 때만)
        var samplePath = System.getenv("COVER_SAMPLE_PATH");
        if (samplePath != null && !samplePath.isBlank()) {
            Files.write(Path.of(samplePath), png);
        }
    }

    @Test
    void bundledFontsRenderKorean() {
        var sample = "취약점 해결 방법을 정리했어요 뷁 똠양꿍";
        assertEquals(-1, CoverRenderer.boldFont().canDisplayUpTo(sample));
        assertEquals(-1, CoverRenderer.regularFont().canDisplayUpTo(sample));
        assertTrue(CoverRenderer.boldFont().getFontName().startsWith("Pretendard"), CoverRenderer.boldFont().getFontName());
    }

    @Test
    void everyThemeAndIconRenders() {
        for (var theme : CoverTheme.values()) {
            for (var icon : CoverIcon.values()) {
                var image = renderer.render(new CoverDesign("아주 긴 제목이 들어와도 줄을 나눠서 잘 들어가야 해요 정말로", "부제목",
                        "", theme, icon), "hwanstory.kr/@test");
                assertEquals(1200, image.getWidth());
            }
        }
    }

    @Test
    void designLimitsAndFallbacks() {
        var design = CoverDesign.of("가".repeat(40), "나".repeat(50), "BADGE-WAY-TOO-LONG-1234567", "no-such-theme", "no-such-icon");
        assertEquals(CoverDesign.HEADLINE_MAX, design.headline().length());
        assertEquals(CoverDesign.SUBHEAD_MAX, design.subhead().length());
        assertEquals(CoverDesign.BADGE_MAX, design.badge().length());
        assertEquals(CoverTheme.SLATE, design.theme());
        assertEquals(CoverIcon.BOOK, design.icon());
        // CVE 번호 라벨은 자르지 않는다
        assertEquals("CVE-2026-114938", CoverDesign.of("x", "", "CVE-2026-114938", "slate", "bug").badge());
    }

    @Test
    void longWordsBreakAtHyphensFirst() {
        var image = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        var fm = g.getFontMetrics(CoverRenderer.boldFont().deriveFont(80f));
        var maxWidth = fm.stringWidth("CVE-2026-") + 5;
        var lines = CoverRenderer.wrap("CVE-2026-114938 자바", fm, maxWidth);
        g.dispose();
        assertEquals("CVE-2026-", lines.get(0));
        assertTrue(lines.get(1).startsWith("114938"), lines.get(1));
        lines.forEach(line -> assertTrue(fm.stringWidth(line) <= maxWidth, line));
    }
}
