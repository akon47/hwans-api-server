package com.hwans.apiserver.controller;

import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.service.blog.BlogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * RSS 2.0 피드 Controller
 */
@RestController
@Api(tags = "RSS")
@RequiredArgsConstructor
public class RssController {
    private final BlogService blogService;

    /**
     * RSS item의 link/guid 생성을 위한 블로그 웹 주소
     */
    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;
    @Value("${blog.rss.title:Hwan'Story}")
    private String feedTitle;
    @Value("${blog.rss.description:직접 만든 블로그 서비스.}")
    private String feedDescription;

    /**
     * RSS 피드에 포함할 최대 게시글 수
     */
    private static final int FEED_SIZE = 30;

    private static final ZoneId FEED_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter PUB_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static final MediaType RSS_MEDIA_TYPE = MediaType.valueOf("application/rss+xml; charset=UTF-8");

    @ApiOperation(value = "전체 RSS 피드", notes = "전체 공개 게시글의 RSS 2.0 피드를 반환한다.", tags = "RSS")
    @GetMapping(value = "/rss", produces = "application/rss+xml")
    public ResponseEntity<String> getGlobalFeed() {
        var posts = blogService.getRecentPublicPosts(null, FEED_SIZE);
        return rssResponse(feedTitle, webBaseUrl, feedDescription, webBaseUrl + "/rss", posts);
    }

    @ApiOperation(value = "특정 블로그 RSS 피드", notes = "특정 블로그 공개 게시글의 RSS 2.0 피드를 반환한다.", tags = "RSS")
    @GetMapping(value = "/rss/{blogId}", produces = "application/rss+xml")
    public ResponseEntity<String> getBlogFeed(@ApiParam(value = "블로그 Id") @PathVariable String blogId) {
        var posts = blogService.getRecentPublicPosts(blogId, FEED_SIZE);
        var channelLink = webBaseUrl + "/" + blogId;
        return rssResponse(feedTitle + " - " + blogId, channelLink, feedDescription, webBaseUrl + "/rss/" + blogId, posts);
    }

    private ResponseEntity<String> rssResponse(String channelTitle, String channelLink, String channelDescription,
                                               String selfLink, List<SimplePostDto> posts) {
        return ResponseEntity.ok()
                .contentType(RSS_MEDIA_TYPE)
                .body(buildRss(channelTitle, channelLink, channelDescription, selfLink, posts));
    }

    private String buildRss(String channelTitle, String channelLink, String channelDescription,
                            String selfLink, List<SimplePostDto> posts) {
        var sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        sb.append("<channel>\n");
        appendElement(sb, "title", channelTitle);
        appendElement(sb, "link", channelLink);
        appendElement(sb, "description", channelDescription);
        appendElement(sb, "language", "ko-KR");
        sb.append("<atom:link href=\"").append(escape(selfLink)).append("\" rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (var post : posts) {
            var blogId = Optional.ofNullable(post.getAuthor()).map(a -> a.getBlogId()).orElse(null);
            if (blogId == null) {
                continue;
            }
            var link = webBaseUrl + "/" + blogId + "/posts/" + post.getPostUrl();
            sb.append("<item>\n");
            appendElement(sb, "title", post.getTitle());
            appendElement(sb, "link", link);
            appendElement(sb, "description", post.getSummary());
            if (post.getAuthor().getName() != null) {
                appendElement(sb, "author", post.getAuthor().getName());
            }
            if (post.getCreatedAt() != null) {
                appendElement(sb, "pubDate", post.getCreatedAt().atZone(FEED_ZONE).format(PUB_DATE_FORMATTER));
            }
            sb.append("<guid isPermaLink=\"true\">").append(escape(link)).append("</guid>\n");
            sb.append("</item>\n");
        }

        sb.append("</channel>\n");
        sb.append("</rss>\n");
        return sb.toString();
    }

    private static void appendElement(StringBuilder sb, String tag, String value) {
        sb.append("<").append(tag).append(">").append(escape(value)).append("</").append(tag).append(">\n");
    }

    /**
     * XML 특수문자를 이스케이프 처리한다.
     */
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
