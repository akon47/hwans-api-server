package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.token.DeviceFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료된 지 하루가 지난 기기 로그인 요청을 매일 새벽에 지운다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceAuthorizationCleanupScheduler {
    private final DeviceFlowService deviceFlowService;

    @Scheduled(cron = "0 40 4 * * *")
    public void purgeExpiredDeviceAuthorizations() {
        var purged = deviceFlowService.purgeExpired();
        if (purged > 0) {
            log.info("purged {} expired device authorizations", purged);
        }
    }
}
