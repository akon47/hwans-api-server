package com.hwans.apiserver.repository.ai;

import com.hwans.apiserver.entity.ai.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 사용 기록 Repository
 */
@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {
    // 기록이 없으면 null
    @Query("select sum(x.costUsd) from AiUsage x where x.account.id = :accountId and x.createdAt >= :since")
    BigDecimal sumCostSince(@Param("accountId") UUID accountId, @Param("since") LocalDateTime since);

    @Query("select count(x) from AiUsage x where x.account.id = :accountId and x.createdAt >= :since")
    long countSince(@Param("accountId") UUID accountId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from AiUsage x where x.account.id = :accountId")
    int deleteAllByAccountId(@Param("accountId") UUID accountId);
}
