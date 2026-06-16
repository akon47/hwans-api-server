package com.hwans.apiserver.controller;

import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.service.blog.BlogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * sitemap.xml Controller (검색엔진 색인용)
 */
@RestController
@Api(tags = "Sitemap")
@RequiredArgsConstructor
public class SitemapController {
    private final BlogService blogService;

    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;

    /**
     * sitemap 에 포함할 최대 게시글 수
     */
    private static final int SITEMAP_SIZE = 2000;

    private static final DateTimeFormatter LASTMOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final MediaType XML_MEDIA_TYPE = MediaType.valueOf("application/xml; charset=UTF-8");

    @ApiOperation(value = "sitemap.xml", notes = "전체 공개 게시글과 블로그 페이지의 sitemap 을 반환한다.", tags = "Sitemap")
    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public ResponseEntity<String> getSitemap() {
        var posts = blogService.getRecentPublicPosts(null, SITEMAP_SIZE);
        return ResponseEntity.ok().contentType(XML_MEDIA_TYPE).body(buildSitemap(posts));
    }

    private String buildSitemap(java.util.List<SimplePostDto> posts) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 메인 페이지
        appendUrl(sb, webBaseUrl + "/", null);

        // 게시글이 있는 블로그들의 루트 페이지 (중복 제거)
        var blogRoots = new LinkedHashSet<String>();
        for (var post : posts) {
            var blogId = Optional.ofNullable(post.getAuthor()).map(a -> a.getBlogId()).orElse(null);
            if (blogId != null) {
                blogRoots.add(blogId);
            }
        }
        for (var blogId : blogRoots) {
            appendUrl(sb, webBaseUrl + "/" + blogId, null);
        }

        // 각 공개 게시글
        for (var post : posts) {
            var blogId = Optional.ofNullable(post.getAuthor()).map(a -> a.getBlogId()).orElse(null);
            if (blogId == null) {
                continue;
            }
            var loc = webBaseUrl + "/" + blogId + "/posts/" + post.getPostUrl();
            var lastmod = post.getLastModifiedAt() != null ? post.getLastModifiedAt().format(LASTMOD_FORMATTER) : null;
            appendUrl(sb, loc, lastmod);
        }

        sb.append("</urlset>\n");
        return sb.toString();
    }

    private static void appendUrl(StringBuilder sb, String loc, String lastmod) {
        sb.append("<url>");
        sb.append("<loc>").append(escape(loc)).append("</loc>");
        if (lastmod != null) {
            sb.append("<lastmod>").append(lastmod).append("</lastmod>");
        }
        sb.append("</url>\n");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
