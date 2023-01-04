package com.hwans.apiserver.dto.mail;

import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 메일 전송을 위한 Dto
 */
@Getter
@Builder
public class MailMessageDto implements Serializable {
    @NotBlank
    private String to;
    @NotBlank
    private String subject;
    @NotBlank
    private String content;
    @Builder.Default
    private Boolean isHtmlContent = false;
}
