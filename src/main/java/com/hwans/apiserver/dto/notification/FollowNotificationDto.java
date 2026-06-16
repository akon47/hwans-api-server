package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

/**
 * 팔로우 알림 Dto
 */
@Getter
@SuperBuilder
@ApiModel(description = "팔로우 알림 Dto")
public class FollowNotificationDto extends NotificationDto {
    @ApiModelProperty(value = "새로 팔로우한 사용자", required = true)
    @NotNull
    SimpleAccountDto follower;
}
