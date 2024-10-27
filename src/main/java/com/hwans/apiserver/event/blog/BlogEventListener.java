package com.hwans.apiserver.event.blog;

import com.hwans.apiserver.service.mail.MailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BlogEventListener {
    private final MailSenderService mailSenderService;

    @Async
    @TransactionalEventListener
    public void onCreateComment(CreateCommentEvent event) {
        var comment = event.getComment();
        var email = comment.getPost().getAuthor().getEmail();
        mailSenderService.sendCreateCommentNotify(email, comment.getId());
    }
}
