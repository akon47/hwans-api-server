package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

/**
 * 팔로우 알림 Dto
 */
@Getter
@SuperBuilder
@Schema(description = "팔로우 알림 Dto")
public class FollowNotificationDto extends NotificationDto {
    @Schema(description = "새로 팔로우한 사용자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    SimpleAccountDto follower;
}
