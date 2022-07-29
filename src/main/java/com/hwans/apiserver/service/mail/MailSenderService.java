package com.hwans.apiserver.service.mail;

import com.hwans.apiserver.dto.mail.MailMessageDto;

public interface MailSenderService {
    void sendMail(MailMessageDto mailMessageDto);
}
