package com.hwans.apiserver.service.mail;

import com.hwans.apiserver.dto.mail.MailMessageDto;

public interface MailSenderService {
    void sendMailVerifyCode(String email, String verifyCode);
    void sendResetPasswordUrl(String email, String resetPasswordToken);
}
