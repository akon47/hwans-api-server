package com.hwans.apiserver.service.mail;

/**
 * 메일 전송 서비스 인터페이스
 */
public interface MailSenderService {
    /**
     * 이메일 인증을 위한 인증코드 메일을 전송합니다.
     *
     * @param email 전송할 대상 메일 주소
     * @param verifyCode 인증코드
     */
    void sendMailVerifyCode(String email, String verifyCode);

    /**
     * 비밀번호 초기화 메일을 전송합니다.
     *
     * @param email 전송할 대상 메일 주소
     * @param resetPasswordToken 비밀번호 초기화를 위한 토큰
     */
    void sendResetPasswordUrl(String email, String resetPasswordToken);
}
