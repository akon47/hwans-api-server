package com.hwans.apiserver.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 초안 끝의 {@code ## 참고 자료} 단락을 찾고, 읽고, 만든다.
 */
public final class AiReferences {
    public static final String HEADING = "## 참고 자료";

    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^#{2,3}\\s*참고\\s*자료\\s*$");
    private static final Pattern NEXT_HEADING_PATTERN = Pattern.compile("(?m)^#{1,2}\\s+\\S");
    // 제목 안의 \[ \] 같은 이스케이프도 허용
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[((?:\\\\.|[^\\]\\\\\\n])+)]\\((https?://[^\\s)]+)\\)");
    private static final Pattern ESCAPED = Pattern.compile("\\\\(.)");
    private static final Pattern BARE_URL = Pattern.compile("(?<![(\\[<])\\bhttps?://[^\\s)\\]>]+");

    private AiReferences() {
    }

    /**
     * 출처 하나 (제목, 주소)
     */
    public record Source(String title, String url) {
    }

    public static boolean hasSection(String body) {
        return body != null && HEADING_PATTERN.matcher(body).find();
    }

    /**
     * {@code ## 참고 자료} 단락의 링크를 읽는다. 단락이 없으면 빈 목록.
     */
    public static List<Source> parseSection(String body) {
        if (body == null) {
            return List.of();
        }
        var heading = HEADING_PATTERN.matcher(body);
        if (!heading.find()) {
            return List.of();
        }
        var section = body.substring(heading.end());
        var next = NEXT_HEADING_PATTERN.matcher(section);
        if (next.find()) {
            section = section.substring(0, next.start());
        }
        var found = new LinkedHashMap<String, Source>();
        var links = MARKDOWN_LINK.matcher(section);
        while (links.find()) {
            var title = ESCAPED.matcher(links.group(1)).replaceAll("$1").trim();
            found.putIfAbsent(links.group(2), new Source(title, links.group(2)));
        }
        var urls = BARE_URL.matcher(MARKDOWN_LINK.matcher(section).replaceAll(""));
        while (urls.find()) {
            var url = urls.group().replaceAll("[.,;:]+$", "");
            found.putIfAbsent(url, new Source(url, url));
        }
        return new ArrayList<>(found.values());
    }

    /**
     * 본문 끝에 붙일 {@code ## 참고 자료} 단락 (앞에 빈 줄 포함)
     */
    public static String buildSection(String body, List<Source> sources) {
        var sb = new StringBuilder();
        if (body != null && !body.isEmpty()) {
            sb.append(body.endsWith("\n\n") ? "" : body.endsWith("\n") ? "\n" : "\n\n");
        }
        sb.append(HEADING).append("\n\n");
        for (var source : sources) {
            var title = source.title() == null || source.title().isBlank() ? source.url() : source.title();
            sb.append("- [").append(escapeTitle(title)).append("](").append(escapeUrl(source.url())).append(")\n");
        }
        return sb.toString();
    }

    private static String escapeTitle(String title) {
        return title.strip().replace("\n", " ").replace("[", "\\[").replace("]", "\\]");
    }

    private static String escapeUrl(String url) {
        return url.replace(" ", "%20").replace("(", "%28").replace(")", "%29");
    }
}
