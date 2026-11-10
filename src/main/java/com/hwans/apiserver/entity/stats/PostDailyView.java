package com.hwans.apiserver.entity.stats;

import com.hwans.apiserver.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 글의 날짜별 조회수 (글 통계 화면의 추이용. 전체 조회수는 Post.hits 가 따로 가진다)
 */
@Entity
@Table(name = "tb_post_daily_view",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_daily_view", columnNames = {"post_id", "view_date"}),
        indexes = @Index(name = "idx_post_daily_view_date", columnList = "view_date"))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDailyView extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(name = "post_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID postId;
    @Column(name = "view_date", nullable = false)
    private LocalDate viewDate;
    @Column(nullable = false)
    private long views;
}
