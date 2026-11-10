package com.hwans.apiserver.service.stats;

import com.hwans.apiserver.dto.stats.PostStatsDto;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.entity.stats.PostDailyView;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.stats.PostDailyViewRepository;
import com.hwans.apiserver.repository.stats.SearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 통계 서비스: 조회·검색을 날짜별로 쌓고, 작성자용 글 통계와 관리자용 인사이트를 만든다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {
    private static final int MAX_DAYS = 90;
    private static final int KEYWORD_MAX_LENGTH = 50;

    private final StatsRecorder statsRecorder;
    private final PostDailyViewRepository postDailyViewRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    /**
     * 글 조회 한 번을 오늘 날짜에 더한다. 통계 기록이 실패해도 글 보기에는 영향을 주지 않는다.
     */
    public void recordPostView(UUID postId) {
        try {
            statsRecorder.increasePostView(postId, LocalDate.now());
        } catch (Exception e) {
            // 같은 글의 오늘 첫 조회가 동시에 들어오면 한쪽이 중복으로 실패한다. 그 한 번은 다시 올려 본다.
            try {
                statsRecorder.increasePostView(postId, LocalDate.now());
            } catch (Exception retryFailure) {
                log.debug("post view stat skipped: {}", retryFailure.getMessage());
            }
        }
    }

    /**
     * 검색어 한 번을 기록한다. (첫 페이지 검색에서만 부른다)
     */
    public void recordSearch(String search) {
        if (search == null) {
            return;
        }
        var keyword = search.strip().replaceAll("\\s+", " ").toLowerCase();
        if (keyword.isEmpty() || keyword.length() > KEYWORD_MAX_LENGTH) {
            return;
        }
        try {
            statsRecorder.increaseSearch(keyword, LocalDateTime.now());
        } catch (Exception e) {
            log.debug("search stat skipped: {}", e.getMessage());
        }
    }

    /**
     * 한 블로그의 글 통계: 기간 동안의 날짜별 조회수와 글별 수치.
     */
    @Transactional(readOnly = true)
    public PostStatsDto.Mine getBlogStats(String blogId, int days) {
        var period = clampDays(days);
        var since = LocalDate.now().minusDays(period - 1L);
        var posts = postRepository.findAllByBlogId(blogId, false);
        var postIds = posts.stream().map(Post::getId).toList();
        List<PostDailyView> views = postIds.isEmpty() ? List.of() : postDailyViewRepository.findByPostIdsSince(postIds, since);

        var viewsByDate = views.stream().collect(Collectors.groupingBy(PostDailyView::getViewDate, Collectors.summingLong(PostDailyView::getViews)));
        var viewsByPost = views.stream().collect(Collectors.groupingBy(PostDailyView::getPostId, Collectors.summingLong(PostDailyView::getViews)));

        var postStats = posts.stream()
                .map(post -> PostStatsDto.PostViews.builder()
                        .post(postMapper.EntityToSimplePostDto(post))
                        .recentViews(viewsByPost.getOrDefault(post.getId(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(PostStatsDto.PostViews::getRecentViews).reversed()
                        .thenComparing(x -> x.getPost().getHits(), Comparator.reverseOrder()))
                .toList();
        return PostStatsDto.Mine.builder()
                .days(period)
                .daily(fillDaily(since, period, viewsByDate))
                .posts(postStats)
                .build();
    }

    /**
     * 사이트 전체 인사이트: 날짜별 조회수, 기간 동안 많이 읽힌 글, 많이 찾는 검색어.
     */
    @Transactional(readOnly = true)
    public PostStatsDto.Insights getInsights(int days) {
        var period = clampDays(days);
        var since = LocalDate.now().minusDays(period - 1L);

        Map<LocalDate, Long> viewsByDate = new HashMap<>();
        postDailyViewRepository.sumViewsByDateSince(since)
                .forEach(row -> viewsByDate.put((LocalDate) row[0], ((Number) row[1]).longValue()));

        var topRows = postDailyViewRepository.sumViewsByPostSince(since, PageRequest.of(0, 10));
        var topIds = topRows.stream().map(row -> (UUID) row[0]).toList();
        Map<UUID, Post> postsById = topIds.isEmpty() ? Map.of()
                : postRepository.findAllById(topIds).stream().collect(Collectors.toMap(Post::getId, Function.identity()));
        var topPosts = topRows.stream()
                .filter(row -> postsById.containsKey((UUID) row[0]) && !postsById.get((UUID) row[0]).isDeleted())
                .map(row -> PostStatsDto.PostViews.builder()
                        .post(postMapper.EntityToSimplePostDto(postsById.get((UUID) row[0])))
                        .recentViews(((Number) row[1]).longValue())
                        .build())
                .toList();

        var topKeywords = searchKeywordRepository.findAllByOrderBySearchCountDescLastSearchedAtDesc(PageRequest.of(0, 15)).stream()
                .map(keyword -> PostStatsDto.Keyword.builder()
                        .keyword(keyword.getKeyword())
                        .count(keyword.getSearchCount())
                        .lastSearchedAt(keyword.getLastSearchedAt())
                        .build())
                .toList();

        return PostStatsDto.Insights.builder()
                .days(period)
                .daily(fillDaily(since, period, viewsByDate))
                .topPosts(topPosts)
                .topKeywords(topKeywords)
                .build();
    }

    private int clampDays(int days) {
        return Math.min(Math.max(days, 7), MAX_DAYS);
    }

    // 기록이 없는 날도 0 으로 채워 오래된 날부터 돌려준다.
    private List<PostStatsDto.DailyViews> fillDaily(LocalDate since, int days, Map<LocalDate, Long> viewsByDate) {
        var result = new ArrayList<PostStatsDto.DailyViews>(days);
        for (int i = 0; i < days; i++) {
            var date = since.plusDays(i);
            result.add(PostStatsDto.DailyViews.builder().date(date).views(viewsByDate.getOrDefault(date, 0L)).build());
        }
        return result;
    }
}
