package com.hwans.apiserver.repository.token;

import com.hwans.apiserver.entity.token.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 개인 액세스 토큰 Repository
 */
@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, UUID> {
    Optional<AccessToken> findByTokenId(String tokenId);

    List<AccessToken> findAllByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    Optional<AccessToken> findByIdAndAccount_Id(UUID id, UUID accountId);

    void deleteAllByAccount_Id(UUID accountId);
}
