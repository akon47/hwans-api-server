package com.hwans.apiserver.service.ai.cover;

/**
 * 커버 이미지에 넣을 문구와 디자인 (Claude 가 구조화 출력으로 고른다)
 *
 * @param headline 큰 글씨 (28자 이내)
 * @param subhead  작은 글씨 한 줄 (40자 이내)
 * @param badge    왼쪽 위 라벨 (12자 안팎, 비어 있으면 그리지 않음. CVE 번호처럼 자르면 안 되는 이름 때문에 서버는 20자까지 받는다)
 * @param theme    배경 테마
 * @param icon     오른쪽 그림
 */
public record CoverDesign(String headline, String subhead, String badge, CoverTheme theme, CoverIcon icon) {
    public static final int HEADLINE_MAX = 28;
    public static final int SUBHEAD_MAX = 40;
    public static final int BADGE_MAX = 20;

    /**
     * 길이 제한을 넘는 글자는 자르고 빈 값은 채운다.
     */
    public static CoverDesign of(String headline, String subhead, String badge, String theme, String icon) {
        return new CoverDesign(
                cut(headline, HEADLINE_MAX),
                cut(subhead, SUBHEAD_MAX),
                cut(badge, BADGE_MAX),
                CoverTheme.fromKey(theme).orElse(CoverTheme.SLATE),
                CoverIcon.fromKey(icon).orElse(CoverIcon.BOOK));
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return "";
        }
        var text = value.replaceAll("\\s+", " ").strip();
        return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max)).strip();
    }
}
