package com.hwans.apiserver.event.blog;

import com.hwans.apiserver.entity.blog.Post;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 게시글 생성 이벤트
 */
@Getter
public class CreatePostEvent extends ApplicationEvent {
    private final Post post;

    public CreatePostEvent(Object source, Post post) {
        super(source);
        this.post = post;
    }
}
