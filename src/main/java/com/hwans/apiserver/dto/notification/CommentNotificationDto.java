package com.hwans.apiserver.dto.notification;

import com.hwans.apiserver.dto.blog.SimpleCommentDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

/**
 * 댓글 알림 Dto
 */
@Getter
@SuperBuilder
@ApiModel(parent = NotificationDto.class)
public class CommentNotificationDto extends NotificationDto {
    @ApiModelProperty(value = "알림을 발생시킨 댓글", required = true)
    @NotNull
    SimpleCommentDto comment;
}
