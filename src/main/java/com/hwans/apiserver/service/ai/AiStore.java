package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.entity.ai.AiSetting;
import com.hwans.apiserver.entity.ai.AiUsage;
import com.hwans.apiserver.entity.ai.AiUsageKind;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.ai.AiSettingRepository;
import com.hwans.apiserver.repository.ai.AiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 설정·사용 기록의 짧은 쓰기 트랜잭션. Anthropic 호출(수 초~수 분)은 트랜잭션 밖에서 하고, 저장만 여기서 한다.
 * 스트리밍 작업 스레드에서도 부르므로 요청 스레드의 영속성 컨텍스트에 기대지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AiStore {
    public static final BigDecimal DEFAULT_MONTHLY_LIMIT_USD = new BigDecimal("10.00");

    private final AccountRepository accountRepository;
    private final AiSettingRepository aiSettingRepository;
    private final AiUsageRepository aiUsageRepository;

    /**
     * 설정을 바꾼다(없으면 만든다). null 인 값은 그대로 둔다.
     */
    @Transactional
    public void saveSettings(UUID accountId, String encryptedApiKey, String keyLast4, AiModel model, BigDecimal monthlyLimitUsd) {
        var setting = findOrCreate(accountId);
        if (encryptedApiKey != null) {
            setting.setApiKey(encryptedApiKey, keyLast4);
        }
        if (model != null) {
            setting.setModel(model.getId());
        }
        if (monthlyLimitUsd != null) {
            setting.setMonthlyLimitUsd(monthlyLimitUsd);
        }
        aiSettingRepository.save(setting);
    }

    @Transactional
    public void clearApiKey(UUID accountId) {
        aiSettingRepository.findByAccount_Id(accountId).ifPresent(setting -> {
            setting.clearApiKey();
            aiSettingRepository.save(setting);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStyle(UUID accountId, String profile, int postCount, LocalDateTime analyzedAt) {
        var setting = findOrCreate(accountId);
        setting.setStyle(profile, postCount, analyzedAt);
        aiSettingRepository.save(setting);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUsage(UUID accountId, AiUsageKind kind, AiModel model, AiTokenUsage usage) {
        var account = accountRepository.getReferenceById(accountId);
        aiUsageRepository.save(AiUsage.create(account, kind, model.getId(),
                usage.inputTokens(), usage.outputTokens(), usage.cacheReadTokens(), usage.cacheWriteTokens(),
                usage.webSearches(), usage.costUsd(model)));
    }

    private AiSetting findOrCreate(UUID accountId) {
        return aiSettingRepository.findByAccount_Id(accountId)
                .orElseGet(() -> AiSetting.create(accountRepository.getReferenceById(accountId), AiModel.DEFAULT.getId(), DEFAULT_MONTHLY_LIMIT_USD));
    }
}
