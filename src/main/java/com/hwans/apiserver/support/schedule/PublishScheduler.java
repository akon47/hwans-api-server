package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 예약 발행된 게시글을 발행 시각에 맞춰 공개로 전환하는 스케줄러
 */
@RequiredArgsConstructor
@Component
public class PublishScheduler {
    private final BlogService blogService;

    // 매 분 0초마다 발행 시각이 지난 예약 게시글을 발행한다.
    @Scheduled(cron = "0 * * * * ?")
    private void publishScheduledPosts() {
        blogService.publishScheduledPosts();
    }
}
