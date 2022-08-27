package com.hwans.apiserver.support.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class HitsScheduler {

    @Scheduled(cron = "0 */1 * * * ?")
    void updateHits() {

    }
}
