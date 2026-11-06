package com.hwans.apiserver.service.seo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.service.blog.BlogService;
import com.hwans.apiserver.service.docs.AgentDocsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 검색엔진·SNS 미리보기·AI 크롤러용 서버 렌더 HTML 을 만든다 (dynamic rendering).
 * 프론트 nginx 가 크롤러의 페이지 요청을 {@code /prerender/<SPA 경로>} 로 넘기면, 같은 주소의 SPA 화면과 같은 내용을
 * 본문·메타·canonical·JSON-LD 가 든 정적 HTML 로 돌려준다. 없는 글·블로그는 진짜 404 를 돌려 soft-404 를 막는다.
 * 사람에게 보이는 화면은 SPA 가 그대로 담당한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrerenderService {
    // 주의: 이 클래스에는 @Transactional 을 붙이지 않는다. 여기서는 BlogService 의 NOT_FOUND 예외를 잡아 404 페이지로 바꾸는데,
    // 바깥 트랜잭션에 묶이면 안쪽 호출의 예외가 트랜잭션을 rollback-only 로 만들어 커밋 시점에 500 이 난다.
    public record Page(int status, String html) {
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final int HOME_SIZE = 30;
    private static final int BLOG_SIZE = 50;
    private static final int RELATED_SIZE = 5;
    private static final Pattern BLOG_PATH = Pattern.compile("^/(@[A-Za-z0-9_-]+)(?:/(posts|likes|followers|followings)(?:/([A-Za-z0-9_-]+))?)?$");
    private static final Set<String> PRIVATE_ROUTES = Set.of(
            "/setting", "/bookmarks", "/signup", "/signin", "/write", "/feed", "/viewing", "/admin", "/admin/live",
            "/social-authentication-redirect", "/reset-password", "/privacy-policy", "/not-found", "/tokens", "/device");
    private static final String STYLE = """
            body{margin:0;font-family:-apple-system,BlinkMacSystemFont,"Noto Sans KR","Apple SD Gothic Neo",sans-serif;line-height:1.75;color:#202020;background:#fafafa;word-break:keep-all}
            header.site{border-bottom:1px solid #ddd;padding:12px 16px;font-size:.95em}
            header.site a{color:#202020;text-decoration:none}header.site a.brand{font-weight:700}
            main{max-width:820px;margin:0 auto;padding:24px 16px}
            h1{font-size:1.9em;line-height:1.3;margin:.2em 0 .4em}h2{font-size:1.4em;margin:1.6em 0 .5em}
            a{color:#218bff}img,video{max-width:100%;height:auto}
            pre{background:#f0f0f0;padding:12px 14px;overflow-x:auto;border-radius:5px;line-height:1.5}
            code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:.9em}
            blockquote{margin:1em 0;padding:.4em 1em;border-left:4px solid #218bff;background:#f3f6fa}
            table{border-collapse:collapse;max-width:100%;overflow-x:auto;display:block}th,td{border:1px solid #ddd;padding:.4em .7em}
            .meta{color:#666;font-size:.9em}.tags a{display:inline-block;margin-right:.5em}
            ul.posts{list-style:none;padding:0;margin:0}ul.posts li{padding:14px 0;border-bottom:1px solid #e5e5e5}
            ul.posts .title{font-size:1.15em;font-weight:600}ul.posts p{margin:.3em 0}
            .series{border:1px solid #ddd;border-radius:5px;padding:12px 16px;margin:1.2em 0;background:#f3f6fa}
            .comment{border-top:1px solid #e5e5e5;padding:.8em 0}.comment .who{font-weight:600}
            footer.site{border-top:1px solid #ddd;color:#666;font-size:.85em;padding:16px;text-align:center}
            """;

    private final BlogService blogService;
    private final AgentDocsService agentDocsService;
    private final MarkdownRenderer markdownRenderer;
    private final ObjectMapper objectMapper;

    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;
    @Value("${blog.api-base-url:https://api.hwanstory.kr}")
    private String apiBaseUrl;
    @Value("${blog.rss.title:Hwan'Story}")
    private String siteName;
    @Value("${blog.rss.description:직접 만든 블로그 서비스.}")
    private String siteDescription;

    /**
     * SPA 경로에 해당하는 서버 렌더 페이지를 만든다.
     *
     * @param rawPath SPA 경로 (예: /@kim-hwan/posts/my-post)
     * @param tag     게시글 목록의 태그 필터 (선택)
     * @return 상태 코드와 HTML
     */
    public Page render(String rawPath, String tag) {
        var path = normalizePath(rawPath);
        try {
            if (path.equals("/")) {
                return home("createdAt", "/", siteName, siteDescription);
            }
            if (path.equals("/top")) {
                return home("hits", "/top", "인기 글 | " + siteName, siteName + " 에서 가장 많이 읽힌 글.");
            }
            if (path.equals("/agents")) {
                return agentGuide();
            }
            var matcher = BLOG_PATH.matcher(path);
            if (matcher.matches()) {
                var blogId = matcher.group(1);
                var section = matcher.group(2);
                var rest = matcher.group(3);
                if (section == null) {
                    return blog(blogId);
                }
                if ("posts".equals(section)) {
                    return rest == null ? blogPosts(blogId, tag) : post(blogId, rest);
                }
                if (rest == null) {
                    // 좋아요·팔로워·팔로잉 목록은 색인 가치가 없다 → noindex, 블로그로 안내
                    return noindexStub(blogId.substring(1) + " · " + section, "/" + blogId);
                }
            }
            if (PRIVATE_ROUTES.contains(path) || path.startsWith("/admin/")) {
                return noindexStub("로그인이 필요한 화면", "/");
            }
            return notFound();
        } catch (RestApiException e) {
            if (e.getErrorCode().getStatus() == HttpStatus.NOT_FOUND) {
                return notFound();
            }
            throw e;
        }
    }

    // ───────────────────────── 페이지들 ─────────────────────────

    private Page home(String sortBy, String path, String title, String description) {
        var posts = blogService.getAllPosts(null, Optional.empty(), HOME_SIZE, sortBy).getData();
        var canonical = webBaseUrl + (path.equals("/") ? "/" : path);
        var jsonLd = Map.<String, Object>of(
                "@context", "https://schema.org",
                "@type", "WebSite",
                "name", siteName,
                "url", webBaseUrl + "/",
                "description", siteDescription,
                "inLanguage", "ko");

        var body = new StringBuilder();
        body.append("<h1>").append(esc(title)).append("</h1>\n");
        body.append("<p class=\"meta\">").append(esc(description)).append("</p>\n");
        body.append("<p class=\"meta\"><a href=\"").append(webBaseUrl).append("/\">최신 글</a> · <a href=\"")
                .append(webBaseUrl).append("/top\">인기 글</a> · <a href=\"").append(apiBaseUrl).append("/rss\">RSS</a></p>\n");
        body.append(postList(posts, true));

        return new Page(200, layout(title, description, canonical, "website", webBaseUrl + "/logo-symbol.png", null, jsonLd, false, body.toString(), null));
    }

    private Page blog(String blogId) {
        var details = blogService.getBlogDetails(blogId, true);
        var owner = details.getOwner();
        var ownerName = displayName(owner);
        var canonical = webBaseUrl + "/" + blogId;
        var title = ownerName + " (" + blogId + ") | " + siteName;
        var description = firstNonBlank(owner.getBiography(), ownerName + "님의 블로그. 게시글 " + details.getPostCount() + "개.");

        PostDto about = null;
        try {
            var found = blogService.getPost(blogId, "about-me");
            if (found.getOpenType() == OpenType.PUBLIC) {
                about = found;
            }
        } catch (RestApiException ignored) {
            // 소개 글이 없을 수 있다.
        }
        var posts = blogService.getBlogPosts(blogId, null, Optional.empty(), BLOG_SIZE, true).getData();
        var image = imageUrl(owner.getProfileImageUrl());

        var person = new LinkedHashMap<String, Object>();
        person.put("@type", "Person");
        person.put("name", ownerName);
        person.put("url", canonical);
        if (image != null) person.put("image", image);
        if (notBlank(owner.getHomepage())) person.put("sameAs", List.of(owner.getHomepage()));
        if (notBlank(owner.getBiography())) person.put("description", owner.getBiography());
        var jsonLd = new LinkedHashMap<String, Object>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "ProfilePage");
        jsonLd.put("url", canonical);
        jsonLd.put("name", title);
        jsonLd.put("mainEntity", person);

        var body = new StringBuilder();
        body.append("<h1>").append(esc(ownerName)).append(" <span class=\"meta\">").append(esc(blogId)).append("</span></h1>\n");
        if (notBlank(owner.getBiography())) {
            body.append("<p>").append(esc(owner.getBiography())).append("</p>\n");
        }
        var facts = new ArrayList<String>();
        if (notBlank(owner.getCompany())) facts.add(esc(owner.getCompany()));
        if (notBlank(owner.getLocation())) facts.add(esc(owner.getLocation()));
        if (notBlank(owner.getHomepage())) facts.add("<a href=\"" + esc(owner.getHomepage()) + "\" rel=\"nofollow noopener\">" + esc(owner.getHomepage()) + "</a>");
        facts.add("게시글 " + details.getPostCount() + "개");
        facts.add("팔로워 " + details.getFollowerCount() + "명");
        body.append("<p class=\"meta\">").append(String.join(" · ", facts)).append("</p>\n");
        if (details.getTagCounts() != null && !details.getTagCounts().isEmpty()) {
            body.append("<p class=\"tags\">");
            for (var tagCount : details.getTagCounts()) {
                body.append("<a href=\"").append(webBaseUrl).append("/").append(blogId).append("/posts?tag=").append(urlParam(tagCount.getName()))
                        .append("\">#").append(esc(tagCount.getName())).append(" (").append(tagCount.getCount()).append(")</a>");
            }
            body.append("</p>\n");
        }
        if (about != null) {
            body.append("<section><h2>").append(esc(about.getTitle())).append("</h2>\n")
                    .append(markdownRenderer.toSafeHtml(about.getContent(), webBaseUrl)).append("</section>\n");
        }
        body.append("<h2>게시글</h2>\n").append(postList(posts, false));
        body.append("<p class=\"meta\"><a href=\"").append(webBaseUrl).append("/").append(blogId).append("/posts\">게시글 전체 보기</a> · <a href=\"")
                .append(apiBaseUrl).append("/rss/").append(blogId).append("\">이 블로그 RSS</a></p>\n");

        return new Page(200, layout(title, description, canonical, "profile", image, blogId, jsonLd, false, body.toString(), null));
    }

    private Page blogPosts(String blogId, String tag) {
        var details = blogService.getBlogDetails(blogId, true);
        var ownerName = displayName(details.getOwner());
        var hasTag = notBlank(tag);
        var posts = blogService.getBlogPosts(blogId, hasTag ? tag : null, Optional.empty(), BLOG_SIZE, true).getData();
        var canonical = webBaseUrl + "/" + blogId + "/posts" + (hasTag ? "?tag=" + urlParam(tag) : "");
        var title = (hasTag ? "#" + tag + " · " : "") + ownerName + "의 게시글 | " + siteName;
        var description = hasTag ? ownerName + "님이 '" + tag + "' 태그로 쓴 글 " + posts.size() + "개." : ownerName + "님의 게시글 " + details.getPostCount() + "개.";
        var jsonLd = Map.<String, Object>of(
                "@context", "https://schema.org",
                "@type", "CollectionPage",
                "name", title,
                "url", canonical,
                "inLanguage", "ko");

        var body = new StringBuilder();
        body.append("<h1>").append(esc(ownerName)).append("의 게시글").append(hasTag ? " <span class=\"meta\">#" + esc(tag) + "</span>" : "").append("</h1>\n");
        body.append("<p class=\"meta\"><a href=\"").append(webBaseUrl).append("/").append(blogId).append("\">").append(esc(blogId)).append(" 블로그</a></p>\n");
        body.append(postList(posts, false));
        if (posts.isEmpty()) {
            body.append("<p class=\"meta\">게시글이 없습니다.</p>\n");
        }

        return new Page(200, layout(title, description, canonical, "website", imageUrl(details.getOwner().getProfileImageUrl()), blogId, jsonLd, false, body.toString(), null));
    }

    private Page post(String blogId, String postUrl) {
        var post = blogService.getPost(blogId, postUrl);
        if (post.getOpenType() != OpenType.PUBLIC) {
            return notFound();
        }
        var author = post.getAuthor();
        var authorName = author == null ? blogId : firstNonBlank(author.getName(), blogId);
        var canonical = webBaseUrl + "/" + blogId + "/posts/" + post.getPostUrl();
        var title = post.getTitle() + " | " + siteName;
        var description = firstNonBlank(post.getSummary(), post.getTitle());
        var image = imageUrl(post.getThumbnailImageUrl());
        var tags = post.getTags() == null ? List.<String>of() : post.getTags().stream().map(TagDto::getName).filter(Objects::nonNull).toList();

        SeriesDto series = post.getSeriesUrl() == null ? null : quietly(() -> blogService.getSeries(blogId, post.getSeriesUrl()), null);
        List<SimplePostDto> related = quietly(() -> blogService.getRelatedPosts(blogId, postUrl, RELATED_SIZE), List.of());
        var comments = post.getComments() == null ? List.<SimpleCommentDto>of() : post.getComments();

        var jsonLd = new LinkedHashMap<String, Object>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "BlogPosting");
        jsonLd.put("headline", post.getTitle());
        jsonLd.put("description", description);
        if (image != null) jsonLd.put("image", List.of(image));
        if (post.getCreatedAt() != null) jsonLd.put("datePublished", iso(post.getCreatedAt()));
        if (post.getLastModifiedAt() != null) jsonLd.put("dateModified", iso(post.getLastModifiedAt()));
        jsonLd.put("author", Map.of("@type", "Person", "name", authorName, "url", webBaseUrl + "/" + blogId));
        jsonLd.put("publisher", Map.of("@type", "Organization", "name", siteName, "logo", Map.of("@type", "ImageObject", "url", webBaseUrl + "/logo-symbol.png")));
        jsonLd.put("mainEntityOfPage", Map.of("@type", "WebPage", "@id", canonical));
        jsonLd.put("url", canonical);
        if (!tags.isEmpty()) jsonLd.put("keywords", String.join(", ", tags));
        jsonLd.put("commentCount", comments.size());
        jsonLd.put("inLanguage", "ko");

        var extraMeta = new StringBuilder();
        if (post.getCreatedAt() != null) extraMeta.append(meta("property", "article:published_time", iso(post.getCreatedAt())));
        if (post.getLastModifiedAt() != null) extraMeta.append(meta("property", "article:modified_time", iso(post.getLastModifiedAt())));
        extraMeta.append(meta("property", "article:author", webBaseUrl + "/" + blogId));
        for (var tag : tags) extraMeta.append(meta("property", "article:tag", tag));

        var body = new StringBuilder();
        body.append("<article>\n<h1>").append(esc(post.getTitle())).append("</h1>\n");
        body.append("<p class=\"meta\"><a href=\"").append(webBaseUrl).append("/").append(blogId).append("\" rel=\"author\">").append(esc(authorName)).append("</a>");
        if (post.getCreatedAt() != null) {
            body.append(" · <time datetime=\"").append(iso(post.getCreatedAt())).append("\">").append(date(post.getCreatedAt())).append("</time>");
        }
        if (post.getLastModifiedAt() != null && post.getCreatedAt() != null && post.getLastModifiedAt().isAfter(post.getCreatedAt().plusMinutes(1))) {
            body.append(" (수정 <time datetime=\"").append(iso(post.getLastModifiedAt())).append("\">").append(date(post.getLastModifiedAt())).append("</time>)");
        }
        body.append(" · 조회 ").append(post.getHits()).append("</p>\n");
        if (!tags.isEmpty()) {
            body.append("<p class=\"tags\">");
            for (var tag : tags) {
                body.append("<a href=\"").append(webBaseUrl).append("/").append(blogId).append("/posts?tag=").append(urlParam(tag)).append("\" rel=\"tag\">#").append(esc(tag)).append("</a>");
            }
            body.append("</p>\n");
        }
        if (series != null && series.getPosts() != null && !series.getPosts().isEmpty()) {
            body.append("<nav class=\"series\"><strong>시리즈 · ").append(esc(series.getTitle())).append("</strong><ol>\n");
            for (var item : series.getPosts()) {
                var isCurrent = Objects.equals(item.getPostUrl(), post.getPostUrl());
                body.append("<li>").append(isCurrent ? "<strong>" : "<a href=\"" + webBaseUrl + "/" + blogId + "/posts/" + esc(item.getPostUrl()) + "\">")
                        .append(esc(item.getTitle())).append(isCurrent ? "</strong>" : "</a>").append("</li>\n");
            }
            body.append("</ol></nav>\n");
        }
        body.append("<div class=\"content\">\n").append(markdownRenderer.toSafeHtml(post.getContent(), webBaseUrl)).append("\n</div>\n</article>\n");
        if (!related.isEmpty()) {
            body.append("<section><h2>관련 게시글</h2>\n").append(postList(related, false)).append("</section>\n");
        }
        if (!comments.isEmpty()) {
            body.append("<section><h2>댓글 ").append(comments.size()).append("개</h2>\n");
            for (var comment : comments) {
                body.append("<div class=\"comment\"><span class=\"who\">").append(esc(comment.getAuthor() == null ? "익명" : firstNonBlank(comment.getAuthor().getName(), "익명"))).append("</span>");
                if (comment.getCreatedAt() != null) {
                    body.append(" <span class=\"meta\">").append(date(comment.getCreatedAt())).append("</span>");
                }
                body.append("<p>").append(esc(comment.getContent()).replace("\n", "<br>")).append("</p></div>\n");
            }
            body.append("</section>\n");
        }

        return new Page(200, layout(title, description, canonical, "article", image, blogId, jsonLd, false, body.toString(), extraMeta.toString()));
    }

    private Page agentGuide() {
        var canonical = webBaseUrl + "/agents";
        var title = "AI 에이전트 가이드 | " + siteName;
        var description = "AI 에이전트에게 " + siteName + " 블로그 글쓰기·검토를 맡길 때 에이전트가 읽는 가이드와 API 레퍼런스.";
        var body = "<p class=\"meta\">원문(마크다운): <a href=\"" + apiBaseUrl + "/agents.md\">" + apiBaseUrl + "/agents.md</a></p>\n"
                + markdownRenderer.toSafeHtml(agentDocsService.text("docs/agents.md"), webBaseUrl);
        var jsonLd = Map.<String, Object>of("@context", "https://schema.org", "@type", "TechArticle", "headline", title, "url", canonical, "inLanguage", "ko");
        return new Page(200, layout(title, description, canonical, "article", webBaseUrl + "/logo-symbol.png", null, jsonLd, false, body, null));
    }

    private Page noindexStub(String what, String backPath) {
        var body = "<h1>" + esc(what) + "</h1>\n<p>이 화면은 앱에서만 볼 수 있습니다. <a href=\"" + webBaseUrl + backPath + "\">돌아가기</a></p>\n";
        return new Page(200, layout(what + " | " + siteName, siteDescription, null, "website", null, null, null, true, body, null));
    }

    private Page notFound() {
        var body = "<h1>페이지를 찾을 수 없습니다</h1>\n<p>주소가 잘못되었거나 글이 삭제·비공개되었습니다. <a href=\"" + webBaseUrl + "/\">" + esc(siteName) + " 홈으로</a></p>\n";
        return new Page(404, layout("페이지를 찾을 수 없습니다 | " + siteName, siteDescription, null, "website", null, null, null, true, body, null));
    }

    // ───────────────────────── 조각들 ─────────────────────────

    private String postList(List<SimplePostDto> posts, boolean showAuthor) {
        var sb = new StringBuilder("<ul class=\"posts\">\n");
        for (var post : posts) {
            var blogId = post.getAuthor() == null ? null : post.getAuthor().getBlogId();
            if (blogId == null) {
                continue;
            }
            var url = webBaseUrl + "/" + blogId + "/posts/" + post.getPostUrl();
            sb.append("<li><a class=\"title\" href=\"").append(esc(url)).append("\">").append(esc(post.getTitle())).append("</a>");
            if (notBlank(post.getSummary())) {
                sb.append("<p>").append(esc(post.getSummary())).append("</p>");
            }
            sb.append("<p class=\"meta\">");
            if (showAuthor) {
                sb.append("<a href=\"").append(webBaseUrl).append("/").append(blogId).append("\">").append(esc(firstNonBlank(post.getAuthor().getName(), blogId))).append("</a> · ");
            }
            if (post.getCreatedAt() != null) {
                sb.append("<time datetime=\"").append(iso(post.getCreatedAt())).append("\">").append(date(post.getCreatedAt())).append("</time>");
            }
            if (post.getTags() != null && !post.getTags().isEmpty()) {
                sb.append(" · ").append(post.getTags().stream().map(TagDto::getName).filter(Objects::nonNull).map(t -> "#" + esc(t)).collect(Collectors.joining(" ")));
            }
            sb.append("</p></li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    /**
     * 공통 레이아웃: head(메타·canonical·OG·JSON-LD) + 상단 바 + 본문 + 하단.
     */
    private String layout(String title, String description, String canonical, String ogType, String image, String blogId,
                          Map<String, Object> jsonLd, boolean noindex, String body, String extraMeta) {
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"ko\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        sb.append("<title>").append(esc(title)).append("</title>\n");
        sb.append(meta("name", "description", description));
        sb.append(meta("name", "robots", noindex ? "noindex, nofollow" : "index, follow, max-image-preview:large, max-snippet:-1"));
        if (canonical != null) {
            sb.append("<link rel=\"canonical\" href=\"").append(esc(canonical)).append("\">\n");
            sb.append(meta("property", "og:url", canonical));
        }
        sb.append(meta("property", "og:site_name", siteName));
        sb.append(meta("property", "og:locale", "ko_KR"));
        sb.append(meta("property", "og:type", ogType));
        sb.append(meta("property", "og:title", title));
        sb.append(meta("property", "og:description", description));
        if (image != null) {
            sb.append(meta("property", "og:image", image));
            sb.append(meta("name", "twitter:image", image));
        }
        sb.append(meta("name", "twitter:card", image != null && "article".equals(ogType) ? "summary_large_image" : "summary"));
        sb.append(meta("name", "twitter:title", title));
        sb.append(meta("name", "twitter:description", description));
        if (extraMeta != null) {
            sb.append(extraMeta);
        }
        sb.append("<link rel=\"alternate\" type=\"application/rss+xml\" title=\"").append(esc(siteName)).append(" RSS\" href=\"").append(apiBaseUrl).append("/rss")
                .append(blogId == null ? "" : "/" + blogId).append("\">\n");
        sb.append("<link rel=\"icon\" href=\"").append(webBaseUrl).append("/favicon.ico\">\n");
        if (jsonLd != null) {
            sb.append("<script type=\"application/ld+json\">").append(toJson(jsonLd)).append("</script>\n");
        }
        sb.append("<style>").append(STYLE).append("</style>\n</head>\n<body>\n");
        sb.append("<header class=\"site\"><a class=\"brand\" href=\"").append(webBaseUrl).append("/\">").append(esc(siteName)).append("</a>");
        if (blogId != null) {
            sb.append(" › <a href=\"").append(webBaseUrl).append("/").append(blogId).append("\">").append(esc(blogId)).append("</a>");
        }
        sb.append("</header>\n<main>\n").append(body).append("</main>\n");
        sb.append("<footer class=\"site\">").append(esc(siteName)).append(" · <a href=\"").append(webBaseUrl).append("/\">").append(webBaseUrl).append("</a></footer>\n");
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private String toJson(Map<String, Object> value) {
        try {
            // </script> 로 스크립트가 닫히지 않도록 < 를 이스케이프한다.
            return objectMapper.writeValueAsString(value).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            log.warn("failed to serialize JSON-LD", e);
            return "{}";
        }
    }

    private static String meta(String attr, String key, String content) {
        return "<meta " + attr + "=\"" + esc(key) + "\" content=\"" + esc(content) + "\">\n";
    }

    private String imageUrl(String attachmentUrl) {
        if (!notBlank(attachmentUrl)) {
            return null;
        }
        return attachmentUrl.startsWith("http") ? attachmentUrl : apiBaseUrl + "/api/v1" + attachmentUrl;
    }

    private static String displayName(AccountDto owner) {
        return firstNonBlank(owner.getName(), owner.getBlogId());
    }

    private static String normalizePath(String rawPath) {
        var path = rawPath == null || rawPath.isBlank() ? "/" : rawPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static <T> T quietly(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String iso(LocalDateTime value) {
        return value.atZone(ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static String date(LocalDateTime value) {
        return value.atZone(ZONE).format(DateTimeFormatter.ofPattern("yyyy. M. d."));
    }

    private static String urlParam(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        return notBlank(first) ? first : second;
    }

    static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
