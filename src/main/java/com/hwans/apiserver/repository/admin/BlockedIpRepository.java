package com.hwans.apiserver.repository.admin;

import com.hwans.apiserver.entity.admin.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, UUID> {
    Optional<BlockedIp> findByIp(String ip);

    List<BlockedIp> findAllByOrderByCreatedAtDesc();
}
