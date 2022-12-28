package com.hwans.apiserver.service.mail;

import com.hwans.apiserver.dto.mail.MailMessageDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}
