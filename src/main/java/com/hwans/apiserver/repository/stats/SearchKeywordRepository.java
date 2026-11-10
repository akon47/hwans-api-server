package com.hwans.apiserver.repository.stats;

import com.hwans.apiserver.entity.stats.SearchKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, String> {
    @Modifying
    @Query("update SearchKeyword k set k.searchCount = k.searchCount + 1, k.lastSearchedAt = :now where k.keyword = :keyword")
    int increase(@Param("keyword") String keyword, @Param("now") LocalDateTime now);

    List<SearchKeyword> findAllByOrderBySearchCountDescLastSearchedAtDesc(Pageable page);
}
