package com.hwans.apiserver.event.blog;

import com.hwans.apiserver.entity.blog.Comment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 댓글 생성 이벤트
 */
@Getter
public class CreateCommentEvent extends ApplicationEvent {
    private final Comment comment;

    public CreateCommentEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }
}
