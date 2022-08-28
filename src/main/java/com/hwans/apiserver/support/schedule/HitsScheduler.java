package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class HitsScheduler {
    private final BlogService blogService;

    @Scheduled(cron = "0 */5 * * * ?")
    private void updatePostHitsFromCache() {
        blogService.updatePostHitsFromCache();
    }
}
