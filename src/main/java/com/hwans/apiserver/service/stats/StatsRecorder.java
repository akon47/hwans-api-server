package com.hwans.apiserver.service.stats;

import com.hwans.apiserver.entity.stats.PostDailyView;
import com.hwans.apiserver.entity.stats.SearchKeyword;
import com.hwans.apiserver.repository.stats.PostDailyViewRepository;
import com.hwans.apiserver.repository.stats.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 통계용 숫자를 하나씩 올리는 저수준 기록기.
 * 각 기록은 자기만의 트랜잭션에서 돌아, 실패해도(예: 같은 줄을 동시에 처음 만들 때의 중복) 부른 쪽 작업을 망치지 않는다.
 * 부르는 쪽은 {@link StatsService} 를 쓴다.
 */
@Service
@RequiredArgsConstructor
public class StatsRecorder {
    private final PostDailyViewRepository postDailyViewRepository;
    private final SearchKeywordRepository searchKeywordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increasePostView(UUID postId, LocalDate date) {
        if (postDailyViewRepository.increase(postId, date) == 0) {
            postDailyViewRepository.saveAndFlush(PostDailyView.builder().postId(postId).viewDate(date).views(1).build());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseSearch(String keyword, LocalDateTime now) {
        if (searchKeywordRepository.increase(keyword, now) == 0) {
            searchKeywordRepository.saveAndFlush(SearchKeyword.builder().keyword(keyword).searchCount(1).lastSearchedAt(now).build());
        }
    }
}
