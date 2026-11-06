package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * 알림 Controller
 */
@RestController
@Tag(name = "알림")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "알림 목록 조회", description = "받은 알림 목록을 조회한다.")
    @GetMapping(value = "/v1/notifications")
    public SliceDto<NotificationDto> getNotifications(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                                      @Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                      @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size,
                                                      @Parameter(description = "읽지 않은 알림만 조회할지 여부") @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        return notificationService.getNotifications(userAuthenticationDetails.getId(), cursorId, size, unreadOnly);
    }

    @Operation(summary = "알림 조회", description = "알림을 조회한다.")
    @GetMapping(value = "/v1/notifications/{notificationId}")
    public NotificationDto getNotification(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                           @Parameter(description = "알림 Id") @PathVariable UUID notificationId) {
        return notificationService.getNotification(userAuthenticationDetails.getId(), notificationId);
    }

    @Operation(summary = "알림 읽기", description = "알림을 읽음 상태로 변경한다.")
    @PutMapping(value = "/v1/notifications/{notificationId}/mark-as-read")
    public NotificationDto markNotificationAsRead(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                   @Parameter(description = "알림 Id") @PathVariable UUID notificationId) {
        return notificationService.markNotificationAsRead(userAuthenticationDetails.getId(), notificationId);
    }

    @Operation(summary = "모든 알림 삭제", description = "모든 알림을 삭제한다.")
    @DeleteMapping(value = "/v1/notifications")
    public void deleteNotifications(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails) {
        notificationService.deleteNotifications(userAuthenticationDetails.getId());
    }

    @Operation(summary = "알림 삭제", description = "알림을 삭제한다.")
    @DeleteMapping(value = "/v1/notifications/{notificationId}")
    public void deleteNotification(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                   @Parameter(description = "알림 Id") @PathVariable UUID notificationId) {
        notificationService.deleteNotification(userAuthenticationDetails.getId(), notificationId);
    }
}
