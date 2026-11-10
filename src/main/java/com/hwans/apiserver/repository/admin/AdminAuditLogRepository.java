package com.hwans.apiserver.repository.admin;

import com.hwans.apiserver.entity.admin.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
}
