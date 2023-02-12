package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.notification.NotificationType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 Dto
 */
@Getter
@SuperBuilder
@ApiModel(subTypes = {NotificationDto.class}, description = "모든 알림 모델에 대한 부모 모델")
public class NotificationDto implements Serializable {
    @ApiModelProperty(value = "알림 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "알림을 받은 사용자", required = true)
    @NotNull
    SimpleAccountDto receiver;
    @ApiModelProperty(value = "알림 유형", required = true)
    @NotNull
    NotificationType notificationType;
    @ApiModelProperty(value = "알림 메세지", required = true)
    @NotBlank
    String message;
    @ApiModelProperty(value = "알림이 발생한 시간", required = true)
    @NotBlank
    LocalDateTime createdAt;
    @ApiModelProperty(value = "알림을 읽은 시간", required = false)
    @NotBlank
    LocalDateTime readAt;
}
