package com.hwans.apiserver.service.mail;

import com.hwans.apiserver.dto.mail.MailMessageDto;
import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.repository.blog.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.util.UUID;

/**
 * 메일 전송 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MailSenderServiceImpl implements MailSenderService {
    private final JavaMailSender javaMailSender;
    private final CommentRepository commentRepository;

    /**
     * 이메일 인증을 위한 인증코드 메일을 전송합니다.
     *
     * @param email      전송할 대상 메일 주소
     * @param verifyCode 인증코드
     */
    @Override
    public void sendMailVerifyCode(String email, String verifyCode) {
        sendMail(createVerifyEmailMessage(email, verifyCode));
    }

    /**
     * 비밀번호 초기화 메일을 전송합니다.
     *
     * @param email              전송할 대상 메일 주소
     * @param resetPasswordToken 비밀번호 초기화를 위한 토큰
     */
    @Override
    public void sendResetPasswordUrl(String email, String resetPasswordToken) {
        sendMail(createResetPasswordMessage(email, resetPasswordToken));
    }

    /**
     * 새 댓글에 대한 알림 메일을 전송합니다.
     *
     * @param email     전송할 대상 메일 주소
     * @param commentId 새 댓글에 대한 Id
     */
    @Override
    public void sendCreateCommentNotify(String email, UUID commentId) {
        var a = commentRepository.findById(commentId);
        commentRepository.findById(commentId).ifPresent(comment -> {
            sendMail(createNewCommentNotifyMessage(email, comment));
        });
    }


    /**
     * 메일을 전송합니다.
     *
     * @param mailMessageDto 메일을 전송하기 위한 데이터 모델
     */
    private void sendMail(MailMessageDto mailMessageDto) {
        var mimeMessage = javaMailSender.createMimeMessage();
        try {
            var mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(mailMessageDto.getTo());
            mimeMessageHelper.setSubject(mailMessageDto.getSubject());
            mimeMessageHelper.setText(mailMessageDto.getContent(), mailMessageDto.getIsHtmlContent());
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 인증코드 메일에 대한 데이터 모델을 생성합니다.
     *
     * @param email      전송할 대상 메일 주소
     * @param verifyCode 인증코드
     * @return 인증코드 메일에 대한 데이터 모델
     */
    private MailMessageDto createVerifyEmailMessage(String email, String verifyCode) {
        try {
            var templateResource = new ClassPathResource("mail-templates/verify-code.html");
            var content = IOUtils.toString(templateResource.getInputStream(), "UTF-8").replace("{{verify-code}}", verifyCode);

            return MailMessageDto.builder()
                    .subject("[Hwan'Story] 회원가입")
                    .content(content)
                    .to(email)
                    .isHtmlContent(true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 비밀번호 초기화 메일을 위한 데이터 모델을 생성합니다.
     *
     * @param email              전송할 대상 메일 주소
     * @param resetPasswordToken 비밀번호 초기화를 위한 토큰
     * @return 비밀번호 재설정 메일에 대한 데이터 모델
     */
    private MailMessageDto createResetPasswordMessage(String email, String resetPasswordToken) {
        try {
            var templateResource = new ClassPathResource("mail-templates/reset-password.html");
            var content = IOUtils.toString(templateResource.getInputStream(), "UTF-8").replace("{{reset-password-token}}", resetPasswordToken);

            return MailMessageDto.builder()
                    .subject("[Hwan'Story] 비밀번호 재설정 요청")
                    .content(content)
                    .to(email)
                    .isHtmlContent(true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 새 댓글 알림 메일을 위한 데이터 모델을 생성합니다.
     *
     * @param email   전송할 대상 메일 주소
     * @param comment 생성된 댓글
     * @return 새 댓글 알림 메일에 대한 데이터 모델
     */
    private MailMessageDto createNewCommentNotifyMessage(String email, Comment comment) {
        try {
            var templateResource = new ClassPathResource("mail-templates/new-comment-notify.html");
            var content = IOUtils.toString(templateResource.getInputStream(), "UTF-8")
                    .replace("{{comment}}", comment.getContent())
                    .replace("{{blog-id}}", comment.getPost().getAuthor().getBlogId())
                    .replace("{{post-url}}", comment.getPost().getPostUrl());

            return MailMessageDto.builder()
                    .subject("[Hwan'Story] 새 댓글 알림")
                    .content(content)
                    .to(email)
                    .isHtmlContent(true)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
