package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.blog.CommentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * 댓글 알림 Dto
 */
@Getter
@SuperBuilder
@Schema(description = "댓글 알림 Dto")
public class CommentNotificationDto extends NotificationDto {
    @Schema(description = "알림을 발생시킨 댓글", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    CommentDto comment;
}
