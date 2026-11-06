package com.hwans.apiserver.repository.token;

import com.hwans.apiserver.entity.token.DeviceAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 기기 로그인 요청 Repository
 */
@Repository
public interface DeviceAuthorizationRepository extends JpaRepository<DeviceAuthorization, UUID> {
    Optional<DeviceAuthorization> findByDeviceCodeHash(String deviceCodeHash);

    Optional<DeviceAuthorization> findFirstByUserCodeAndStatusOrderByCreatedAtDesc(String userCode, DeviceAuthorization.Status status);

    boolean existsByUserCodeAndStatus(String userCode, DeviceAuthorization.Status status);

    long deleteAllByExpiresAtBefore(LocalDateTime before);

    void deleteAllByAccount_Id(UUID accountId);
}
