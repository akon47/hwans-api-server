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
}
