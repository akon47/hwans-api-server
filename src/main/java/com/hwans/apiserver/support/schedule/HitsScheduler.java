package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 게시글 조회수에 대해서 캐시된 내용을 DB에 반영하는 스케줄러
 */
@RequiredArgsConstructor
@Component
public class HitsScheduler {
    private final BlogService blogService;

    @Scheduled(cron = "0 */5 * * * ?")
    private void updatePostHitsFromCache() {
        blogService.updatePostHitsFromCache();
    }
}
