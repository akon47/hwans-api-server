package com.hwans.apiserver.service.seo;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 게시글 마크다운(Toast UI Editor, GFM)을 크롤러용 HTML 로 바꾼다.
 * 글에 섞인 생(raw) HTML 은 그대로 두되 jsoup 허용 목록으로 정화하여 스크립트·이벤트 핸들러를 제거한다.
 */
@Component
public class MarkdownRenderer {
    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create(),
                TaskListItemsExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
        this.safelist = Safelist.relaxed()
                .addTags("del", "s", "hr", "input", "details", "summary", "figure", "figcaption", "kbd", "mark", "video", "source")
                .addAttributes(":all", "class", "id")
                .addAttributes("input", "type", "checked", "disabled")
                .addAttributes("a", "target", "rel")
                .addAttributes("img", "width", "height", "loading")
                .addAttributes("video", "src", "controls", "width", "height", "poster")
                .addAttributes("source", "src", "type")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("video", "src", "http", "https")
                .addProtocols("source", "src", "http", "https")
                .preserveRelativeLinks(true);
    }

    /**
     * 마크다운을 정화된 HTML 조각으로 바꾼다.
     *
     * @param markdown 마크다운 본문
     * @param baseUri  상대 링크 기준 주소
     * @return HTML
     */
    public String toSafeHtml(String markdown, String baseUri) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        var html = renderer.render(parser.parse(markdown));
        return Jsoup.clean(html, baseUri == null ? "" : baseUri, safelist);
    }
}
