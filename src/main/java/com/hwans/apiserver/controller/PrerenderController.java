package com.hwans.apiserver.controller;

import com.hwans.apiserver.service.seo.PrerenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 검색엔진·SNS 미리보기·AI 크롤러용 서버 렌더 페이지 (인증 없음).
 * 프론트 nginx 가 크롤러의 페이지 요청 {@code /<SPA 경로>} 를 {@code /prerender/<SPA 경로>} 로 넘긴다.
 * 예: {@code GET /prerender/@kim-hwan/posts/my-post} → 그 글의 본문·메타·JSON-LD 가 든 HTML (없으면 404).
 */
@RestController
@ApiIgnore
@RequiredArgsConstructor
public class PrerenderController {
    private static final String PREFIX = "/prerender";
    private static final MediaType HTML = MediaType.valueOf("text/html;charset=UTF-8");

    private final PrerenderService prerenderService;

    @GetMapping(value = {PREFIX, PREFIX + "/**"})
    public ResponseEntity<String> render(HttpServletRequest request,
                                         @RequestParam(required = false) String tag) {
        var uri = request.getRequestURI();
        var path = uri.length() > PREFIX.length() ? URLDecoder.decode(uri.substring(PREFIX.length()), StandardCharsets.UTF_8) : "/";
        var page = prerenderService.render(path, tag);
        var cacheControl = page.status() == 200 ? CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic() : CacheControl.noStore();
        return ResponseEntity.status(page.status())
                .contentType(HTML)
                .cacheControl(cacheControl)
                .header("Vary", "User-Agent")
                .body(page.html());
    }
}
