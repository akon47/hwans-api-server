package com.hwans.apiserver.common.security.token;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessTokenAccessRulesTest {
    private static final Set<String> ALL_SCOPES = Set.of("posts", "social");

    @Test
    void aiWritingIsDeniedForAccessTokens() {
        var cases = new String[][]{
                {"GET", "/api/v1/ai/settings"},
                {"PUT", "/api/v1/ai/settings"},
                {"DELETE", "/api/v1/ai/settings/key"},
                {"POST", "/api/v1/ai/style/analyze"},
                {"POST", "/api/v1/ai/draft"},
                {"POST", "/api/v1/ai/rewrite"},
                {"POST", "/api/v1/ai/meta"},
                {"POST", "/api/v1/ai/translate"},
                {"POST", "/api/v1/ai/cover"},
                {"HEAD", "/api/v1/ai/settings"},
        };
        for (var c : cases) {
            assertEquals(AccessTokenAccessRules.Kind.DENY, AccessTokenAccessRules.decide(c[0], c[1], ALL_SCOPES).kind(), c[0] + " " + c[1]);
        }
    }

    @Test
    void otherRulesAreUnchanged() {
        assertEquals(AccessTokenAccessRules.Kind.ALLOW, AccessTokenAccessRules.decide("GET", "/api/v1/blog/someone/posts", Set.of()).kind());
        assertEquals(AccessTokenAccessRules.Kind.ALLOW, AccessTokenAccessRules.decide("POST", "/api/v1/blog/posts", Set.of("posts")).kind());
        assertEquals(AccessTokenAccessRules.Kind.SCOPE, AccessTokenAccessRules.decide("POST", "/api/v1/blog/posts", Set.of("social")).kind());
        assertEquals(AccessTokenAccessRules.Kind.ALLOW, AccessTokenAccessRules.decide("POST", "/api/v1/attachments", Set.of("posts")).kind());
        assertEquals(AccessTokenAccessRules.Kind.DENY, AccessTokenAccessRules.decide("GET", "/api/v1/tokens", ALL_SCOPES).kind());
    }
}
