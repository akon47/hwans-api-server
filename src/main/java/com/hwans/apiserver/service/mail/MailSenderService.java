package com.hwans.apiserver.service.mail;

/**
 * 메일 전송 서비스 인터페이스
 */
public interface MailSenderService {
    void sendMailVerifyCode(String email, String verifyCode);

    void sendResetPasswordUrl(String email, String resetPasswordToken);
}
