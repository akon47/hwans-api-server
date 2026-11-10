package com.hwans.apiserver.entity.stats;

import com.hwans.apiserver.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 검색어별 검색 횟수 (관리자 대시보드의 "많이 찾는 검색어"용. 누가 검색했는지는 남기지 않는다)
 */
@Entity
@Table(name = "tb_search_keyword")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchKeyword extends BaseEntity {
    // 소문자로 정리한 검색어
    @Id
    @Column(length = 100)
    private String keyword;
    @Column(nullable = false)
    private long searchCount;
    private LocalDateTime lastSearchedAt;
}
