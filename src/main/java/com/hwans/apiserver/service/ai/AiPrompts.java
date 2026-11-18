package com.hwans.apiserver.service.ai;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 글쓰기에 쓰는 프롬프트(한국어).
 * 시스템 프롬프트는 회원별로 바뀌지 않는 부분(지침·문체·예전 글)만 담아 프롬프트 캐시가 잘 걸리게 하고,
 * 요청마다 바뀌는 것(요청 내용, 날짜, 웹 조사 여부)은 사용자 메시지에 둔다.
 */
public final class AiPrompts {
    private AiPrompts() {
    }

    /**
     * 예전 글 한 편 (제목, 본문 발췌)
     */
    public record PostExcerpt(String title, String content) {
    }

    // ===== 초안 =====

    public static final String WRITING_GUIDELINES = """
            당신은 블로그 서비스 Hwan'Story 의 한 회원(블로그 주인)을 대신해 블로그 글 초안을 쓰는 작가입니다.
            완성된 초안은 회원이 읽고 고친 뒤 직접 공개합니다.

            <작성 지침>
            1. 블로그 주인 본인이 쓴 것처럼 씁니다. 아래 <문체 프로필>과 <예전 글>의 말투·어미·글 구성·자주 쓰는 표현·코드 쓰는 법을 따릅니다.
               문체 프로필이 없으면 친근하고 명확한 해요체 기술 블로그 문체로 씁니다. 예전 글의 내용을 그대로 옮기지는 않습니다.
            2. 정확하게 씁니다. 버전·날짜·수치·이름 같은 사실은 확실한 것만 쓰고, 웹 자료가 있으면 그 자료에 나온 것만 씁니다.
               자료에서 확인하지 못한 사실은 지어내지 말고 "아직 공식 정보가 없어요"처럼 솔직하게 씁니다. 추측은 추측이라고 밝힙니다.
            3. 보안 취약점(CVE 등)을 다루는 글이면 다음을 빠짐없이 다룹니다.
               - 무엇이 문제인지(취약점의 종류와 원인을 이해할 수 있는 수준으로)
               - 영향받는 버전과 고쳐진 버전
               - 심각도(CVSS 점수·등급, 공식 출처 기준)
               - 해결 방법(업그레이드)과 당장 할 수 있는 완화 방법
               - 내 환경이 영향받는지 확인하는 방법(버전 확인 명령, 의존성 확인 방법 등)
               근거는 NVD, 제조사·프로젝트의 보안 공지, GitHub Security Advisory 같은 공식 출처를 우선합니다.
               공격 재현 코드·익스플로잇·페이로드·우회 방법은 절대 쓰지 않습니다. 버전 확인 명령이나 안전한 설정 예시는 괜찮습니다.
            4. 형식은 마크다운입니다.
               - 본문만 씁니다. 맨 위에 `#` 제목을 쓰지 말고 도입부부터 시작합니다(제목은 따로 정합니다).
               - 단락 제목은 `##`, 그 아래는 `###` 를 씁니다.
               - 코드 블록에는 언어 이름을 붙입니다(```java, ```bash, ```yaml 등).
            5. 글 본문 말고는 아무것도 쓰지 않습니다. "초안입니다", "검색해 볼게요" 같은 설명·인사·작업 과정 설명을 붙이지 않습니다.
            </작성 지침>
            """;

    public static final String NO_STYLE = "아직 분석된 문체가 없습니다. 친근하고 명확한 해요체 기술 블로그 문체로 쓰세요.";

    /**
     * 초안용 시스템 프롬프트 (회원별로 고정 — 프롬프트 캐시 대상)
     */
    public static String draftSystem(String authorName, String blogId, String styleProfile, List<PostExcerpt> recentPosts) {
        var sb = new StringBuilder(WRITING_GUIDELINES);
        sb.append("\n<블로그 주인>").append(nullToEmpty(authorName)).append(" (@").append(nullToEmpty(blogId)).append(")</블로그 주인>\n");
        sb.append("\n<문체 프로필>\n").append(styleProfile == null || styleProfile.isBlank() ? NO_STYLE : styleProfile.strip()).append("\n</문체 프로필>\n");
        if (!recentPosts.isEmpty()) {
            sb.append("\n<예전 글>\n");
            for (var post : recentPosts) {
                sb.append("<글 제목=\"").append(escapeAttr(post.title())).append("\">\n")
                        .append(post.content().strip()).append("\n</글>\n");
            }
            sb.append("</예전 글>\n");
        }
        return sb.toString();
    }

    public static String lengthGuide(String length) {
        return switch (length) {
            case "short" -> "짧게 (공백 포함 1,500자 안팎)";
            case "long" -> "길게 (공백 포함 6,000자 이상, 충분히 자세하게)";
            default -> "보통 (공백 포함 3,000자 안팎)";
        };
    }

    public static String draftUser(String request, String notes, String length, boolean webResearch, LocalDate today) {
        var sb = new StringBuilder();
        sb.append("오늘 날짜: ").append(today).append("\n\n");
        sb.append("<쓸 내용>\n").append(request.strip()).append("\n</쓸 내용>\n");
        if (notes != null && !notes.isBlank()) {
            sb.append("\n<메모·개요>\n").append(notes.strip()).append("\n</메모·개요>\n");
        }
        sb.append("\n분량: ").append(lengthGuide(length)).append("\n");
        if (webResearch) {
            sb.append("""

                    웹 조사: web_search 와 web_fetch 로 최신 자료를 먼저 충분히 조사한 뒤 쓰세요.
                    - 보안 취약점이면 NVD(nvd.nist.gov), 제조사·프로젝트 보안 공지, GitHub Security Advisory 를 직접 찾아 읽으세요.
                    - 조사하는 동안에는 아무 글도 쓰지 말고, 조사를 마친 뒤 본문만 쓰세요.
                    - 본문 맨 끝에 `## 참고 자료` 단락을 두고, 실제로 참고한 자료를 `- [제목](URL)` 목록으로 쓰세요.
                    - 자료에서 확인하지 못한 내용은 지어내지 말고 "아직 공식 정보가 없어요"처럼 쓰세요.
                    """);
        } else {
            sb.append("\n웹 조사 없이 쓰는 글입니다. 최신 정보(버전, 날짜, 수치)가 필요한 부분은 확실한 것만 쓰고, 확실하지 않으면 확인이 필요하다고 밝히세요.\n");
        }
        sb.append("\n위 지침과 문체대로 블로그 글 본문(마크다운)만 쓰세요.");
        return sb.toString();
    }

    // ===== 문체 분석 =====

    public static final String STYLE_SYSTEM = """
            당신은 글쓰기 문체를 분석하는 편집자입니다. 한 사람의 글 여러 편을 읽고, 다른 작가가 그 사람처럼 쓸 수 있도록
            '문체 프로필'을 한국어로 정리합니다. 글의 주제나 내용을 요약하는 것이 아니라 '어떻게 쓰는지'를 씁니다.
            """;

    public static String styleUser(List<PostExcerpt> posts) {
        var sb = new StringBuilder();
        sb.append("아래는 한 블로그 회원이 직접 쓴 글 ").append(posts.size()).append("편입니다.\n\n");
        for (var post : posts) {
            sb.append("<글 제목=\"").append(escapeAttr(post.title())).append("\">\n").append(post.content().strip()).append("\n</글>\n\n");
        }
        sb.append("""
                이 사람의 문체 프로필을 써 주세요.
                - 2000자 이내, 한국어, 다음 항목을 `1.`~`6.` 번호 제목으로 나눠 씁니다:
                  1. 문체·어미 (존댓말/반말, 자주 쓰는 종결 어미, 문장 길이, 말투)
                  2. 글 구성 (도입·본론·마무리 방식, 단락 제목 쓰는 법, 목록·표 사용)
                  3. 제목 짓는 법
                  4. 코드·예시 쓰는 법 (코드 블록, 명령어, 스크린샷·링크 설명 방식)
                  5. 자주 쓰는 표현 (실제 표현을 짧게 인용)
                  6. 피할 것 (이 사람이 쓰지 않는 표현·형식)
                - 구체적인 예를 들어 쓰고, 일반론은 쓰지 않습니다.
                - 프로필 본문만 출력합니다(인사·설명 없이).
                """);
        return sb.toString();
    }

    // ===== 다듬기 =====

    public static String rewriteSystem(String styleProfile) {
        return """
                당신은 블로그 회원의 글을 다듬는 편집자입니다.
                - 회원의 문체(아래 문체 프로필)를 유지합니다.
                - 바꾼 텍스트만 출력합니다. 설명·머리말·따옴표·코드 펜스로 감싸기 없이, 원래 텍스트 자리에 그대로 넣을 수 있게 씁니다.
                - 마크다운 서식(단락 제목, 목록, 코드 블록, 링크, 이미지)은 유지합니다. 코드 블록 안의 코드는 지시가 없으면 바꾸지 않습니다.
                - 사실을 새로 지어내지 않습니다. 공격(익스플로잇) 코드는 쓰지 않습니다.

                <문체 프로필>
                """ + (styleProfile == null || styleProfile.isBlank() ? NO_STYLE : styleProfile.strip()) + "\n</문체 프로필>\n";
    }

    public static String rewriteInstruction(String instruction, String customInstruction) {
        return switch (instruction) {
            case "polish" -> "문장을 자연스럽고 매끄럽게 다듬어 주세요. 뜻과 분량은 거의 그대로 둡니다.";
            case "expand" -> "내용을 더 자세하게 늘려 주세요(설명·예시 추가). 확실하지 않은 사실은 더하지 않습니다.";
            case "shorten" -> "핵심만 남기고 짧게 줄여 주세요.";
            case "fix" -> "맞춤법·띄어쓰기·문법 오류만 고쳐 주세요. 표현과 문체는 그대로 둡니다.";
            default -> customInstruction.strip();
        };
    }

    /**
     * 프론트가 앞뒤 문맥에 넣는 표시. 바꿀 텍스트가 원래 있던 자리다.
     */
    public static final String REWRITE_MARKER = "[다듬을 부분]";

    public static String rewriteUser(String text, String instruction, String context) {
        var sb = new StringBuilder();
        if (context != null && !context.isBlank()) {
            sb.append("<앞뒤 문맥>\n").append(context.strip()).append("\n</앞뒤 문맥>\n")
                    .append("앞뒤 문맥은 참고만 하세요. 문맥 안의 `").append(REWRITE_MARKER)
                    .append("` 표시가 아래 <바꿀 텍스트>가 들어가는 자리입니다. 앞뒤 글과 자연스럽게 이어지도록 바꾸세요.\n\n");
        }
        sb.append("<바꿀 텍스트>\n").append(text).append("\n</바꿀 텍스트>\n\n");
        sb.append("지시: ").append(instruction).append("\n\n")
                .append("<바꿀 텍스트>를 대신할 텍스트만 출력하세요. `").append(REWRITE_MARKER)
                .append("` 표시, 앞뒤 문맥, 태그, 설명은 출력하지 마세요.");
        return sb.toString();
    }

    // ===== 제목·요약·태그·주소 =====

    public static final String META_SYSTEM = """
            당신은 블로그 편집자입니다. 글을 읽고 제목·요약·태그·주소(slug)를 정해 JSON 으로 답합니다.
            - title: 한국어 제목, 60자 안팎(넘지 않게). 글쓴이의 문체에 맞추고 낚시성 표현은 피합니다.
            - summary: 한국어 요약 80~160자, 1~2문장. 검색 결과·목록에 보일 문장입니다.
            - tags: 3~6개. 아래 '이 블로그에서 쓰는 태그'에 같은 뜻의 태그가 있으면 그 표기를 그대로 씁니다(대소문자·띄어쓰기 포함).
              새 태그는 짧은 고유명사·기술 이름 위주로 씁니다(예: Java, Spring Boot, 보안).
            - postUrl: 영어 소문자·숫자·하이픈만 쓴 짧은 주소(3~8단어, 예: java-cve-2026-114938-fix).
            """;

    public static String metaUser(String title, String content, List<String> existingTags) {
        var sb = new StringBuilder();
        sb.append("이 블로그에서 쓰는 태그(많이 쓴 순): ")
                .append(existingTags.isEmpty() ? "(없음)" : String.join(", ", existingTags)).append("\n\n");
        if (title != null && !title.isBlank()) {
            sb.append("지금 제목(있으면 참고): ").append(title.strip()).append("\n\n");
        }
        sb.append("<글>\n").append(content.strip()).append("\n</글>");
        return sb.toString();
    }

    // ===== 영어 번역 =====

    public static final String TRANSLATE_SYSTEM = """
            You translate Korean technical blog posts into natural, fluent English for an international developer audience.
            - Keep the Markdown structure exactly: headings, lists, tables, links, images, and code blocks.
            - Do not translate code inside code blocks; you may translate code comments. Keep URLs and image paths unchanged.
            - Keep the author's tone (friendly, clear), but write idiomatic English rather than a literal translation.
            - Output only the translated Markdown body, with no preface, notes, or title line.
            """;

    public static String translateUser(String title, String summary, String content) {
        var sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append("Title (for context only, do not output it): ").append(title.strip()).append("\n");
        }
        if (summary != null && !summary.isBlank()) {
            sb.append("Summary (for context only, do not output it): ").append(summary.strip()).append("\n");
        }
        sb.append("\nTranslate this Markdown body into English:\n\n<body>\n").append(content).append("\n</body>");
        return sb.toString();
    }

    public static final String TRANSLATE_META_SYSTEM = """
            You translate a Korean blog post's title and summary into natural English. Answer in JSON.
            - title: concise English title (about 70 characters at most).
            - summary: English summary in 1-2 sentences (about 100-200 characters).
            """;

    public static String translateMetaUser(String title, String summary, String englishBodyStart) {
        return "Korean title: " + nullToEmpty(title).strip()
                + "\nKorean summary: " + nullToEmpty(summary).strip()
                + "\n\nBeginning of the English translation of the body (for consistent terms):\n" + englishBodyStart;
    }

    // ===== 커버 이미지 =====

    public static final String COVER_SYSTEM = """
            당신은 블로그 글의 대표 이미지(1200×630, 글 맨 위와 목록 썸네일에 쓰임)를 꾸미는 디자이너입니다.
            글 정보를 보고 이미지에 넣을 문구와 디자인을 골라 JSON 으로 답합니다.
            - headline: 28자 이내. 이미지의 큰 글씨. 제목을 그대로 쓰지 말고 핵심만 짧게(한국어).
            - subhead: 40자 이내. headline 아래 작은 글씨로 덧붙일 한 줄(한국어).
            - badge: 12자 이내. 왼쪽 위 작은 라벨(예: "CVE-2026-114938", "Java", "Spring Boot"). CVE 번호처럼 자르면 안 되는 이름은 그대로 씁니다. 마땅치 않으면 빈 문자열.
            - theme: security-red(보안·취약점·장애), ocean-blue(일반 개발·클라우드·웹), forest-green(튜토리얼·성장·환경),
                     sunset-orange(회고·이야기·이벤트), violet(AI·디자인·아이디어), slate(인프라·운영·차분한 주제) 중 하나.
            - icon: shield(보안), bug(버그·취약점), lock(인증·암호), code(코드), server(서버), cloud(클라우드), chart(데이터·성능),
                    book(공부·정리), rocket(출시·성능 향상), terminal(명령줄·셸), database(DB), network(네트워크) 중 하나.
            """;

    public static String coverUser(String title, String summary, List<String> tags) {
        return "제목: " + nullToEmpty(title).strip()
                + "\n요약: " + nullToEmpty(summary).strip()
                + "\n태그: " + (tags == null || tags.isEmpty() ? "(없음)" : String.join(", ", tags));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String escapeAttr(String value) {
        return nullToEmpty(value).replace("\"", "'").replace("\n", " ");
    }
}
