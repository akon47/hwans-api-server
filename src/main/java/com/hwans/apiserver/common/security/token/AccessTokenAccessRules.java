package com.hwans.apiserver.common.security.token;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.entity.token.AccessTokenScope;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * 개인 액세스 토큰으로 할 수 있는 요청을 정한다 (위에서부터 먼저 맞는 규칙 적용).
 * <ul>
 *   <li>읽기(GET/HEAD/OPTIONS)는 토큰이 유효하면 모두 가능 (관리자 API, 계정·인증·토큰 관리 제외)</li>
 *   <li>게시글·시리즈·파일 쓰기는 {@code posts} 권한</li>
 *   <li>댓글·좋아요·북마크·이모지 반응·팔로우·알림 쓰기는 {@code social} 권한</li>
 *   <li>계정 정보/비밀번호 변경, 탈퇴, 토큰 발급·폐기(자기 토큰 폐기 제외), 기기 로그인 승인, 관리자 기능은 토큰으로 불가 — 사람이 웹에서 한다</li>
 * </ul>
 */
public final class AccessTokenAccessRules {
    public enum Kind {ALLOW, DENY, SCOPE}

    public record Decision(Kind kind, AccessTokenScope scope) {
        static final Decision ALLOW = new Decision(Kind.ALLOW, null);
        static final Decision DENY = new Decision(Kind.DENY, null);
    }

    private record Rule(Set<String> methods, String pattern, Decision decision) {
        boolean matches(String method, String uri) {
            return (methods == null || methods.contains(method)) && MATCHER.match(pattern, uri);
        }
    }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final String API = Constants.API_PREFIX;
    private static final Set<String> READ = Set.of("GET", "HEAD", "OPTIONS");
    private static final Set<String> WRITE = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final List<Rule> RULES = List.of(
            // 에이전트의 "나는 누구인가" 와 자기 토큰 폐기
            new Rule(Set.of("GET"), API + "/v1/accounts/me", Decision.ALLOW),
            new Rule(Set.of("GET", "DELETE"), API + "/v1/tokens/current", Decision.ALLOW),
            // 사람만 할 수 있는 일
            new Rule(null, API + "/v1/accounts/**", Decision.DENY),
            new Rule(null, API + "/v1/admin/**", Decision.DENY),
            new Rule(null, API + "/v1/authentication/**", Decision.DENY),
            new Rule(null, API + "/v1/tokens/**", Decision.DENY),
            new Rule(null, API + "/chat/**", Decision.DENY),
            // 읽기는 모두 허용
            new Rule(READ, "/**", Decision.ALLOW),
            // posts: 게시글·시리즈·파일
            new Rule(WRITE, API + "/v1/blog/posts/**", scope(AccessTokenScope.POSTS)),
            new Rule(WRITE, API + "/v1/blog/series/**", scope(AccessTokenScope.POSTS)),
            new Rule(WRITE, API + "/v1/attachments", scope(AccessTokenScope.POSTS)),
            // social: 댓글·좋아요·북마크·이모지 반응·팔로우·알림
            new Rule(WRITE, API + "/v1/blog/comments/**", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/blog/*/posts/*/comments/**", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/blog/*/posts/*/likes", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/blog/*/posts/*/bookmarks", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/blog/*/posts/*/reactions", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/follow/**", scope(AccessTokenScope.SOCIAL)),
            new Rule(WRITE, API + "/v1/notifications/**", scope(AccessTokenScope.SOCIAL)),
            // 그 밖의 쓰기는 불가
            new Rule(null, "/**", Decision.DENY)
    );

    private AccessTokenAccessRules() {
    }

    private static Decision scope(AccessTokenScope scope) {
        return new Decision(Kind.SCOPE, scope);
    }

    /**
     * 요청을 이 토큰 권한으로 할 수 있는지 판단한다.
     *
     * @param method     HTTP 메서드
     * @param requestUri 요청 URI (쿼리 제외)
     * @param scopeKeys  토큰이 가진 권한 키 목록
     * @return ALLOW 면 진행, DENY 면 토큰으로 불가, SCOPE 면 {@code scope} 권한이 없어 불가
     */
    public static Decision decide(String method, String requestUri, Set<String> scopeKeys) {
        var upperMethod = method == null ? "" : method.toUpperCase();
        for (var rule : RULES) {
            if (!rule.matches(upperMethod, requestUri)) {
                continue;
            }
            var decision = rule.decision();
            if (decision.kind() == Kind.SCOPE && scopeKeys.contains(decision.scope().getKey())) {
                return Decision.ALLOW;
            }
            return decision;
        }
        return Decision.DENY;
    }
}
