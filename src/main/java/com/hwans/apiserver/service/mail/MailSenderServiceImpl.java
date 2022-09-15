package com.hwans.apiserver.service.mail;

import com.hwans.apiserver.dto.mail.MailMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;

@Service
@RequiredArgsConstructor
public class MailSenderServiceImpl implements MailSenderService {
    private final JavaMailSender javaMailSender;

    @Override
    public void sendMailVerifyCode(String email, String verifyCode) {
        sendMail(createVerifyEmailMessage(email, verifyCode));
    }

    @Override
    public void sendResetPasswordUrl(String email, String resetPasswordToken) {
        sendMail(createResetPasswordMessage(email, resetPasswordToken));
    }

    private void sendMail(MailMessageDto mailMessageDto) {
        var mimeMessage = javaMailSender.createMimeMessage();
        try {
            var mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setFrom("noreply@kimhwan.kr");
            mimeMessageHelper.setTo(mailMessageDto.getTo());
            mimeMessageHelper.setSubject(mailMessageDto.getSubject());
            mimeMessageHelper.setText(mailMessageDto.getContent(), mailMessageDto.getIsHtmlContent());
            javaMailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private MailMessageDto createVerifyEmailMessage(String email, String verifyCode) {
        return MailMessageDto.builder()
                .subject("[Hwan'Story] 회원가입")
                .content(new StringBuilder()
                        .append("코드: ")
                        .append(verifyCode)
                        .toString())
                .to(email)
                .isHtmlContent(false)
                .build();
    }

    private MailMessageDto createResetPasswordMessage(String email, String resetPasswordToken) {
        return MailMessageDto.builder()
                .subject("[Hwan'Story] 비밀번호 재설정 요청")
                .content(new StringBuilder()
                        .append("<div>아래 링크를 눌러 비밀번호 재설정이 가능합니다. 본인이 요청한게 아니라면 이 메일은 무시해 주세요.</div>")
                        .append("<a href=\"https://blog.kimhwan.kr/reset-password?token=") // TODO: 링크 주소에 대해서 설정 파일에 설정하도록 수정 필요.
                        .append(resetPasswordToken)
                        .append("\" target=\"_blank\">비밀번호 재설정</a>")
                        .toString())
                .to(email)
                .isHtmlContent(true)
                .build();
    }
}
