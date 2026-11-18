package com.hwans.apiserver.repository.ai;

import com.hwans.apiserver.entity.ai.AiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 회원별 AI 글쓰기 설정 Repository
 */
@Repository
public interface AiSettingRepository extends JpaRepository<AiSetting, UUID> {
    Optional<AiSetting> findByAccount_Id(UUID accountId);

    @Modifying
    @Query("delete from AiSetting x where x.account.id = :accountId")
    int deleteAllByAccountId(@Param("accountId") UUID accountId);
}
