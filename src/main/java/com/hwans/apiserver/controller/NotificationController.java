package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.notification.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * 알림 Controller
 */
@RestController
@Api(tags = "알림")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @ApiOperation(value = "알림 목록 조회", notes = "받은 알림 목록을 조회한다.", tags = "알림")
    @GetMapping(value = "/v1/notification/notifications")
    public SliceDto<NotificationDto> getNotifications(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                                      @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                      @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size,
                                                      @ApiParam(value = "읽지 않은 알림만 조회할지 여부") @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        return notificationService.getNotifications(userAuthenticationDetails.getId(), cursorId, size, unreadOnly);
    }

    @ApiOperation(value = "알림 조회", notes = "알림을 조회한다.", tags = "알림")
    @GetMapping(value = "/v1/notification/notifications/{notificationId}")
    public NotificationDto getNotification(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                           @ApiParam(value = "알림 Id") @PathVariable UUID notificationId) {
        return notificationService.getNotification(userAuthenticationDetails.getId(), notificationId);
    }

    @ApiOperation(value = "알림 삭제", notes = "알림을 삭제한다.", tags = "알림")
    @DeleteMapping(value = "/v1/notification/notifications/{notificationId}")
    public void deleteNotification(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                   @ApiParam(value = "알림 Id") @PathVariable UUID notificationId) {
        notificationService.deleteNotification(userAuthenticationDetails.getId(), notificationId);
    }
}
