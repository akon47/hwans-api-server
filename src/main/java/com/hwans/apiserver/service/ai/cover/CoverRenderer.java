package com.hwans.apiserver.service.ai.cover;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 커버 이미지(1200×630 PNG)를 Java2D 로 그린다. 글꼴은 저장소에 넣은 Pretendard(OFL, {@code resources/fonts})를 쓰므로
 * 서버에 한글 글꼴이 없어도 된다(헤드리스). 배경 그라데이션 + 라벨 + 큰 글씨 + 작은 글씨 + 선 그림 + 아래쪽 블로그 주소.
 */
@Component
public class CoverRenderer {
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 630;

    private static final int MARGIN_X = 80;
    private static final int TEXT_MAX_WIDTH = 680;
    private static final double ICON_CX = 960;
    private static final double ICON_CY = 292;
    private static final double ICON_RADIUS = 168;
    private static final int FOOTER_LINE_Y = 532;
    private static final int TEXT_BOTTOM = 506;
    private static final Color WHITE = Color.WHITE;

    static {
        // 서버(도커)에는 화면이 없다. 스프링 부트도 기본으로 켜지만 단독 실행(테스트)에서도 안전하게.
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
    }

    private static final class Fonts {
        static final Font BOLD = load("/fonts/Pretendard-Bold.ttf");
        static final Font REGULAR = load("/fonts/Pretendard-Regular.ttf");

        private static Font load(String path) {
            try (var in = CoverRenderer.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("font not found on classpath: " + path);
                }
                return Font.createFont(Font.TRUETYPE_FONT, in);
            } catch (IOException | FontFormatException e) {
                throw new IllegalStateException("failed to load font " + path, e);
            }
        }
    }

    static Font boldFont() {
        return Fonts.BOLD;
    }

    static Font regularFont() {
        return Fonts.REGULAR;
    }

    /**
     * PNG 바이트로 그린다.
     *
     * @param design 문구·테마·그림
     * @param footer 아래쪽에 작게 쓸 글 (예: hwanstory.kr/@blogId)
     */
    public byte[] renderPng(CoverDesign design, String footer) {
        var image = render(design, footer);
        try (var out = new ByteArrayOutputStream(256 * 1024)) {
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("no PNG writer available");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public BufferedImage render(CoverDesign design, String footer) {
        var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setFont(Fonts.REGULAR.deriveFont(Font.PLAIN, 24f));

            drawBackground(g, design.theme());
            drawIcon(g, design.icon(), design.theme());
            var textTop = drawBadge(g, design.badge());
            drawTexts(g, design.headline(), design.subhead(), textTop);
            drawFooter(g, footer);
        } finally {
            g.dispose();
        }
        return image;
    }

    private void drawBackground(Graphics2D g, CoverTheme theme) {
        g.setPaint(new GradientPaint(0, 0, theme.getFrom(), WIDTH, HEIGHT, theme.getTo()));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 그림 뒤쪽의 은은한 빛
        var accent = theme.getAccent();
        g.setPaint(new RadialGradientPaint((float) ICON_CX, (float) ICON_CY, 460f,
                new float[]{0f, 1f},
                new Color[]{withAlpha(accent, 80), withAlpha(accent, 0)}));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 큰 고리 장식
        g.setStroke(new BasicStroke(2f));
        g.setColor(withAlpha(WHITE, 20));
        g.draw(new Ellipse2D.Double(ICON_CX - 250, ICON_CY - 250, 500, 500));
        g.setColor(withAlpha(WHITE, 12));
        g.draw(new Ellipse2D.Double(ICON_CX - 330, ICON_CY - 330, 660, 660));

        // 오른쪽 위 점 무늬
        g.setColor(withAlpha(WHITE, 28));
        for (var x = 0; x < 8; x++) {
            for (var y = 0; y < 4; y++) {
                g.fill(new Ellipse2D.Double(1150 - x * 22, 34 + y * 22, 5, 5));
            }
        }
    }

    private void drawIcon(Graphics2D g, CoverIcon icon, CoverTheme theme) {
        var disc = new Ellipse2D.Double(ICON_CX - ICON_RADIUS, ICON_CY - ICON_RADIUS, ICON_RADIUS * 2, ICON_RADIUS * 2);
        g.setColor(withAlpha(WHITE, 26));
        g.fill(disc);
        g.setStroke(new BasicStroke(3f));
        g.setColor(withAlpha(WHITE, 60));
        g.draw(disc);

        var saved = g.getTransform();
        var scale = 1.1;
        g.translate(ICON_CX - 100 * scale, ICON_CY - 100 * scale);
        g.scale(scale, scale);
        g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(WHITE, 245));
        icon.draw(g);
        g.setTransform(saved);
    }

    /**
     * @return 큰 글씨를 시작할 수 있는 가장 위 y
     */
    private int drawBadge(Graphics2D g, String badge) {
        if (badge == null || badge.isBlank()) {
            return 84;
        }
        var font = Fonts.BOLD.deriveFont(Font.PLAIN, 26f);
        var fm = g.getFontMetrics(font);
        var text = ellipsize(badge, fm, TEXT_MAX_WIDTH - 44);
        var width = fm.stringWidth(text) + 44;
        var height = 50;
        var top = 72;
        var shape = new RoundRectangle2D.Double(MARGIN_X, top, width, height, height, height);
        g.setColor(withAlpha(WHITE, 40));
        g.fill(shape);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(withAlpha(WHITE, 90));
        g.draw(shape);
        g.setFont(font);
        g.setColor(WHITE);
        var baseline = top + (height - fm.getHeight()) / 2f + fm.getAscent();
        g.drawString(text, MARGIN_X + 22f, baseline);
        return top + height + 34;
    }

    private void drawTexts(Graphics2D g, String headline, String subhead, int top) {
        var head = fitHeadline(g, headline == null ? "" : headline);
        var subFont = Fonts.REGULAR.deriveFont(Font.PLAIN, 32f);
        var subMetrics = g.getFontMetrics(subFont);
        var subLines = subhead == null || subhead.isBlank()
                ? List.<String>of()
                : limitLines(wrap(subhead, subMetrics, TEXT_MAX_WIDTH), 2, subMetrics, TEXT_MAX_WIDTH);
        var subLineHeight = 32 * 1.4;
        var gap = subLines.isEmpty() ? 0 : 26;

        var blockHeight = head.lines.size() * head.lineHeight + gap + subLines.size() * subLineHeight;
        var y = Math.max(top, top + (TEXT_BOTTOM - top - blockHeight) / 2.0);

        g.setFont(head.font);
        g.setColor(WHITE);
        for (var line : head.lines) {
            g.drawString(line, (float) MARGIN_X, (float) (y + head.metrics.getAscent()));
            y += head.lineHeight;
        }
        y += gap;
        g.setFont(subFont);
        g.setColor(withAlpha(WHITE, 215));
        for (var line : subLines) {
            g.drawString(line, (float) MARGIN_X, (float) (y + subMetrics.getAscent()));
            y += subLineHeight;
        }
    }

    private record Headline(Font font, FontMetrics metrics, List<String> lines, double lineHeight) {
    }

    /**
     * 3줄 안에 들어가는 가장 큰 글씨 크기를 고른다. 단어(예: CVE 번호)를 쪼개지 않아도 되는 크기를 먼저 찾는다.
     */
    private Headline fitHeadline(Graphics2D g, String text) {
        for (var allowSplitWords : new boolean[]{false, true}) {
            for (var size = 80; size >= 46; size -= 2) {
                var font = Fonts.BOLD.deriveFont(Font.PLAIN, (float) size);
                var fm = g.getFontMetrics(font);
                if (!allowSplitWords && !everyWordFits(text, fm, TEXT_MAX_WIDTH)) {
                    continue;
                }
                var lines = wrap(text, fm, TEXT_MAX_WIDTH);
                // 큰 글씨는 2줄까지, 작아지면 3줄까지
                if (lines.size() <= (size > 64 ? 2 : 3)) {
                    return new Headline(font, fm, lines, size * 1.24);
                }
            }
        }
        var font = Fonts.BOLD.deriveFont(Font.PLAIN, 46f);
        var fm = g.getFontMetrics(font);
        return new Headline(font, fm, limitLines(wrap(text, fm, TEXT_MAX_WIDTH), 3, fm, TEXT_MAX_WIDTH), 46 * 1.24);
    }

    private void drawFooter(Graphics2D g, String footer) {
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(withAlpha(WHITE, 40));
        g.draw(new Line2D.Double(MARGIN_X, FOOTER_LINE_Y, WIDTH - MARGIN_X, FOOTER_LINE_Y));
        if (footer == null || footer.isBlank()) {
            return;
        }
        var font = Fonts.REGULAR.deriveFont(Font.PLAIN, 26f);
        var fm = g.getFontMetrics(font);
        g.setFont(font);
        g.setColor(withAlpha(WHITE, 200));
        g.drawString(ellipsize(footer, fm, WIDTH - MARGIN_X * 2), (float) MARGIN_X, FOOTER_LINE_Y + 28f + fm.getAscent());
    }

    /**
     * 띄어쓰기 단위로 줄을 나누고, 한 단어가 너무 길면 글자 단위로 나눈다.
     */
    static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (var word : text.strip().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            var candidate = current.isEmpty() ? word : current + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (fm.stringWidth(word) <= maxWidth) {
                current.append(word);
                continue;
            }
            // 긴 단어는 하이픈 뒤에서 먼저 나누고(CVE-2026-114938 → CVE-2026- / 114938), 그래도 길면 글자 단위로 쪼갠다
            for (var piece : word.split("(?<=-)")) {
                if (fm.stringWidth(current + piece) <= maxWidth) {
                    current.append(piece);
                    continue;
                }
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                for (var cp : piece.codePoints().toArray()) {
                    var next = current + new String(Character.toChars(cp));
                    if (fm.stringWidth(next) > maxWidth && !current.isEmpty()) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    current.appendCodePoint(cp);
                }
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static boolean everyWordFits(String text, FontMetrics fm, int maxWidth) {
        for (var word : text.strip().split("\\s+")) {
            if (fm.stringWidth(word) > maxWidth) {
                return false;
            }
        }
        return true;
    }

    private static List<String> limitLines(List<String> lines, int max, FontMetrics fm, int maxWidth) {
        if (lines.size() <= max) {
            return lines;
        }
        var kept = new ArrayList<>(lines.subList(0, max));
        kept.set(max - 1, ellipsize(kept.get(max - 1) + "…", fm, maxWidth));
        return kept;
    }

    private static String ellipsize(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        var base = text.endsWith("…") ? text.substring(0, text.length() - 1) : text;
        while (!base.isEmpty() && fm.stringWidth(base + "…") > maxWidth) {
            base = base.substring(0, base.offsetByCodePoints(base.length(), -1));
        }
        return base.strip() + "…";
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }}
