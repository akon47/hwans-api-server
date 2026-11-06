package com.hwans.apiserver.entity.token;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 개인 액세스 토큰 {@code hsp_<tokenId>_<secret>}.
 * AI 에이전트·스크립트가 API 를 쓸 때 사용하며, 발급한 사용자의 권한 안에서 고른 범위(scope)만 할 수 있다.
 * 토큰 원문은 저장하지 않고 전체 토큰의 SHA-256 만 저장한다. tokenId 는 공개 값(조회·화면·로그용).
 */
@Entity
@Table(name = "tb_access_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessToken extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;
    @Column(length = 16, unique = true, nullable = false)
    private String tokenId;
    @Column(length = 64, nullable = false)
    private String tokenHash;
    @Column(length = 100, nullable = false)
    private String name;
    @Column(length = 200, nullable = false)
    private String scopes;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private AccessTokenOrigin origin;
    @Column
    private LocalDateTime expiresAt;
    @Column
    private LocalDateTime lastUsedAt;
    @Column(length = 64)
    private String lastUsedIp;
    @Column
    private LocalDateTime revokedAt;

    public static AccessToken create(Account account, String tokenId, String tokenHash, String name,
                                     Set<AccessTokenScope> scopes, AccessTokenOrigin origin, LocalDateTime expiresAt) {
        var token = new AccessToken();
        token.account = account;
        token.tokenId = tokenId;
        token.tokenHash = tokenHash;
        token.name = name;
        token.scopes = AccessTokenScope.join(scopes);
        token.origin = origin;
        token.expiresAt = expiresAt;
        return token;
    }

    public Set<String> getScopeKeys() {
        return Arrays.stream(scopes.trim().split("\\s+"))
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean hasScope(AccessTokenScope scope) {
        return getScopeKeys().contains(scope.getKey());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = LocalDateTime.now();
        }
    }

    public void used(String ip) {
        this.lastUsedAt = LocalDateTime.now();
        this.lastUsedIp = ip;
    }
}
