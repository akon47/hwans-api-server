package com.hwans.apiserver.entity.ai;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * AI 요청 한 번(이어 받기·보조 호출 포함)의 토큰 사용량과 비용. 시각은 {@link BaseEntity#getCreatedAt()}.
 */
@Entity
@Table(name = "tb_ai_usage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiUsage extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;
    // draft | rewrite | meta | translate | style | cover
    @Column(length = 16, nullable = false)
    private String kind;
    @Column(length = 64, nullable = false)
    private String model;
    @Column(nullable = false)
    private long inputTokens;
    @Column(nullable = false)
    private long outputTokens;
    @Column(nullable = false)
    private long cacheReadTokens;
    @Column(nullable = false)
    private long cacheWriteTokens;
    @Column(nullable = false)
    private int webSearchCount;
    @Column(precision = 12, scale = 6, nullable = false)
    private BigDecimal costUsd;

    public static AiUsage create(Account account, AiUsageKind kind, String model, long inputTokens, long outputTokens,
                                 long cacheReadTokens, long cacheWriteTokens, int webSearchCount, BigDecimal costUsd) {
        var usage = new AiUsage();
        usage.account = account;
        usage.kind = kind.getKey();
        usage.model = model;
        usage.inputTokens = inputTokens;
        usage.outputTokens = outputTokens;
        usage.cacheReadTokens = cacheReadTokens;
        usage.cacheWriteTokens = cacheWriteTokens;
        usage.webSearchCount = webSearchCount;
        usage.costUsd = costUsd;
        return usage;
    }
}
