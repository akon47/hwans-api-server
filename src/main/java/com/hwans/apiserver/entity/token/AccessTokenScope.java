package com.hwans.apiserver.entity.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 개인 액세스 토큰의 권한(scope).
 * 읽기는 토큰이 유효하기만 하면 되고, 쓰기는 아래 권한 중 해당하는 것이 있어야 한다.
 */
@Getter
@RequiredArgsConstructor
public enum AccessTokenScope {
    /**
     * 게시글·시리즈 작성/수정/삭제, 파일(이미지) 업로드
     */
    POSTS("posts", "게시글·시리즈 작성/수정/삭제, 파일 업로드"),
    /**
     * 댓글 작성/수정/삭제, 좋아요·북마크·이모지 반응, 팔로우, 알림 읽음/삭제
     */
    SOCIAL("social", "댓글·좋아요·북마크·이모지 반응·팔로우, 알림 읽음/삭제");

    private final String key;
    private final String description;

    public static Optional<AccessTokenScope> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        var normalized = key.trim().toLowerCase();
        return Arrays.stream(values()).filter(x -> x.key.equals(normalized)).findFirst();
    }

    /**
     * "posts social" / "posts,social" 형태의 문자열을 권한 집합으로 바꾼다. 비어 있으면 모든 권한.
     *
     * @throws IllegalArgumentException 알 수 없는 권한이 있는 경우
     */
    public static Set<AccessTokenScope> parse(String raw) {
        var result = new LinkedHashSet<AccessTokenScope>();
        if (raw != null) {
            for (var token : raw.trim().split("[\\s,]+")) {
                if (token.isEmpty()) {
                    continue;
                }
                result.add(fromKey(token).orElseThrow(() -> new IllegalArgumentException("알 수 없는 권한입니다: " + token + " (posts, social)")));
            }
        }
        if (result.isEmpty()) {
            result.addAll(Arrays.asList(values()));
        }
        return result;
    }

    /**
     * 권한 키 목록을 권한 집합으로 바꾼다.
     *
     * @throws IllegalArgumentException 알 수 없는 권한이 있거나 비어 있는 경우
     */
    public static Set<AccessTokenScope> fromKeys(Iterable<String> keys) {
        var result = new LinkedHashSet<AccessTokenScope>();
        if (keys != null) {
            for (var key : keys) {
                result.add(fromKey(key).orElseThrow(() -> new IllegalArgumentException("알 수 없는 권한입니다: " + key + " (posts, social)")));
            }
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("권한(scope)을 하나 이상 골라야 합니다: posts, social");
        }
        return result;
    }

    public static String join(Set<AccessTokenScope> scopes) {
        return scopes.stream().map(AccessTokenScope::getKey).collect(Collectors.joining(" "));
    }
}
