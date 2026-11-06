package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.notification.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 Dto
 */
@Getter
@SuperBuilder
@Schema(description = "모든 알림 모델에 대한 부모 모델")
public class NotificationDto implements Serializable {
    @Schema(description = "알림 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "알림을 받은 사용자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    SimpleAccountDto receiver;
    @Schema(description = "알림 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    NotificationType notificationType;
    @Schema(description = "알림이 발생한 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    LocalDateTime createdAt;
    @Schema(description = "알림을 읽은 시간")
    @NotBlank
    LocalDateTime readAt;
}
