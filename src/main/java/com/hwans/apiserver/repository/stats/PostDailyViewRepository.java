package com.hwans.apiserver.repository.stats;

import com.hwans.apiserver.entity.stats.PostDailyView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostDailyViewRepository extends JpaRepository<PostDailyView, UUID> {
    @Modifying
    @Query("update PostDailyView v set v.views = v.views + 1 where v.postId = :postId and v.viewDate = :viewDate")
    int increase(@Param("postId") UUID postId, @Param("viewDate") LocalDate viewDate);

    @Query("select v from PostDailyView v where v.postId in :postIds and v.viewDate >= :since")
    List<PostDailyView> findByPostIdsSince(@Param("postIds") Collection<UUID> postIds, @Param("since") LocalDate since);

    // [날짜, 조회수 합] 목록
    @Query("select v.viewDate, sum(v.views) from PostDailyView v where v.viewDate >= :since group by v.viewDate")
    List<Object[]> sumViewsByDateSince(@Param("since") LocalDate since);

    // [글 Id, 조회수 합] 목록 (많은 순)
    @Query("select v.postId, sum(v.views) from PostDailyView v where v.viewDate >= :since group by v.postId order by sum(v.views) desc")
    List<Object[]> sumViewsByPostSince(@Param("since") LocalDate since, org.springframework.data.domain.Pageable page);
}
