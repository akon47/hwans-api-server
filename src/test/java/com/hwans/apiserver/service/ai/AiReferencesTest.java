package com.hwans.apiserver.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiReferencesTest {
    @Test
    void parsesTheReferenceSection() {
        var body = """
                본문 [중간 링크](https://not-a-reference.example.com) 이에요.

                ## 참고 자료

                - [NVD - CVE-2026-114938](https://nvd.nist.gov/vuln/detail/CVE-2026-114938)
                - Oracle 보안 공지: https://www.oracle.com/security-alerts/cpu2026.html.
                - [NVD 중복](https://nvd.nist.gov/vuln/detail/CVE-2026-114938)
                """;
        assertTrue(AiReferences.hasSection(body));
        assertEquals(List.of(
                new AiReferences.Source("NVD - CVE-2026-114938", "https://nvd.nist.gov/vuln/detail/CVE-2026-114938"),
                new AiReferences.Source("https://www.oracle.com/security-alerts/cpu2026.html", "https://www.oracle.com/security-alerts/cpu2026.html")
        ), AiReferences.parseSection(body));
    }

    @Test
    void buildsASectionThatParsesBack() {
        var sources = List.of(
                new AiReferences.Source("GitHub [Advisory]", "https://github.com/advisories/GHSA-abcd-1234"),
                new AiReferences.Source("", "https://example.com/a(b)"));
        var body = "본문 끝.";
        var full = body + AiReferences.buildSection(body, sources);
        assertEquals("""
                본문 끝.

                ## 참고 자료

                - [GitHub \\[Advisory\\]](https://github.com/advisories/GHSA-abcd-1234)
                - [https://example.com/a(b)](https://example.com/a%28b%29)
                """, full);
        assertTrue(AiReferences.hasSection(full));
        assertEquals(2, AiReferences.parseSection(full).size());
    }

    @Test
    void noSection() {
        assertFalse(AiReferences.hasSection("본문만 있어요.\n## 마무리\n끝"));
        assertEquals(List.of(), AiReferences.parseSection("본문만"));
    }

    @Test
    void metaHelpers() throws Exception {
        assertEquals("java-cve-2026-114938-fix", AiServiceImpl.slug(" Java CVE_2026 114938 -- Fix! "));
        assertTrue(AiServiceImpl.slug("한글만").startsWith("post-"));
        var tags = new ObjectMapper().readTree("[\"java\", \"#보안\", \"Spring boot\", \"JAVA\", \"\", \"새태그\"]");
        assertEquals(List.of("Java", "보안", "Spring Boot", "새태그"),
                AiServiceImpl.normalizeTags(tags, List.of("Java", "Spring Boot", "보안")));
    }
}
