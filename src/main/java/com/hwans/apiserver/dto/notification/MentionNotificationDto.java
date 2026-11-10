package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.blog.CommentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * 언급 알림 Dto
 */
@Getter
@SuperBuilder
@Schema(description = "언급 알림 Dto")
public class MentionNotificationDto extends NotificationDto {
    @Schema(description = "나를 언급한 댓글", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    CommentDto comment;
}
