package com.hwans.apiserver.repository.admin;

import com.hwans.apiserver.entity.admin.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findAllByStatus(Report.Status status, Pageable pageable);

    boolean existsByTargetTypeAndTargetIdAndReporterIpAndStatus(Report.TargetType targetType, String targetId, String reporterIp, Report.Status status);

    List<Report> findAllByTargetTypeAndTargetIdAndStatus(Report.TargetType targetType, String targetId, Report.Status status);

    long countByStatus(Report.Status status);

    long countByTargetAuthorEmail(String targetAuthorEmail);

    long countByReporterEmail(String reporterEmail);
}
