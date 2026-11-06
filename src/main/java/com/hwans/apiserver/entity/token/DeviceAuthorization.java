package com.hwans.apiserver.entity.token;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 기기 로그인(RFC 8628 형태) 요청 하나.
 * 에이전트는 비밀인 device_code 를 들고 있고, 사람은 짧은 user_code 를 웹에서 승인/거부한다.
 * 승인 뒤 에이전트의 다음 폴링에서 토큰을 발급하고(ISSUED, 1회용) 끝난다. 토큰 원문은 어디에도 저장되지 않는다.
 */
@Entity
@Table(name = "tb_device_authorization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceAuthorization extends BaseEntity {
    public enum Status {PENDING, APPROVED, DENIED, ISSUED}

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 64, unique = true, nullable = false)
    private String deviceCodeHash;
    @Column(length = 16, nullable = false)
    private String userCode;
    @Column(length = 100, nullable = false)
    private String clientName;
    @Column(length = 200, nullable = false)
    private String scopes;
    @Column(length = 64)
    private String requestedIp;
    @Column(length = 300)
    private String requestedUa;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;
    @Column
    private Integer tokenTtlDays;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column
    private LocalDateTime lastPolledAt;

    public static DeviceAuthorization create(String deviceCodeHash, String userCode, String clientName, String scopes,
                                             String requestedIp, String requestedUa, LocalDateTime expiresAt) {
        var authorization = new DeviceAuthorization();
        authorization.deviceCodeHash = deviceCodeHash;
        authorization.userCode = userCode;
        authorization.clientName = clientName;
        authorization.scopes = scopes;
        authorization.requestedIp = requestedIp;
        authorization.requestedUa = requestedUa == null ? null : requestedUa.substring(0, Math.min(300, requestedUa.length()));
        authorization.status = Status.PENDING;
        authorization.expiresAt = expiresAt;
        return authorization;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public void approve(Account by, Integer ttlDays) {
        this.status = Status.APPROVED;
        this.account = by;
        this.tokenTtlDays = ttlDays;
    }

    public void deny(Account by) {
        this.status = Status.DENIED;
        this.account = by;
    }

    public void issued() {
        this.status = Status.ISSUED;
    }

    public void polled() {
        this.lastPolledAt = LocalDateTime.now();
    }
}
