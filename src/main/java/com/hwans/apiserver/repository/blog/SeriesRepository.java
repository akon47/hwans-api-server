package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.entity.blog.Series;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesRepository extends JpaRepository<Series, UUID> {
    @Query("select x from Series as x where x.account.blogId = :blogId and x.seriesUrl = :seriesUrl")
    Optional<Series> findByBlogIdAndSeriesUrl(String blogId, String seriesUrl);

    @Query("select x from Series as x where x.account.blogId = :blogId")
    List<Series> findByBlogId(String blogId);
}
