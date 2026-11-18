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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원별 AI 글쓰기 설정 (회원당 한 행).
 * Anthropic API 키는 AES-256-GCM 으로 암호화해 두고(원문은 저장하지 않음) 화면에는 끝 4자리만 보여 준다.
 * 문체 프로필은 회원의 예전 글을 Claude 가 분석해 만든 한국어 설명이다.
 */
@Entity
@Table(name = "tb_ai_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSetting extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;
    // "v1:" + base64(IV 12바이트 + 암호문 + GCM 태그)
    @Column(length = 1024)
    private String encryptedApiKey;
    @Column(length = 8)
    private String keyLast4;
    @Column(length = 64, nullable = false)
    private String model;
    @Column(precision = 8, scale = 2, nullable = false)
    private BigDecimal monthlyLimitUsd;
    @Column(length = 4000)
    private String styleProfile;
    @Column
    private LocalDateTime styleAnalyzedAt;
    @Column
    private Integer stylePostCount;

    public static AiSetting create(Account account, String model, BigDecimal monthlyLimitUsd) {
        // id 는 JPA 가 생성하도록 비워 둔다.
        var setting = new AiSetting();
        setting.account = account;
        setting.model = model;
        setting.monthlyLimitUsd = monthlyLimitUsd;
        return setting;
    }

    public boolean hasApiKey() {
        return encryptedApiKey != null && !encryptedApiKey.isEmpty();
    }

    public void setApiKey(String encryptedApiKey, String keyLast4) {
        this.encryptedApiKey = encryptedApiKey;
        this.keyLast4 = keyLast4;
    }

    public void clearApiKey() {
        this.encryptedApiKey = null;
        this.keyLast4 = null;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMonthlyLimitUsd(BigDecimal monthlyLimitUsd) {
        this.monthlyLimitUsd = monthlyLimitUsd;
    }

    public void setStyle(String styleProfile, int postCount, LocalDateTime analyzedAt) {
        this.styleProfile = styleProfile;
        this.stylePostCount = postCount;
        this.styleAnalyzedAt = analyzedAt;
    }

    public boolean hasStyle() {
        return styleProfile != null && !styleProfile.isBlank();
    }
}
