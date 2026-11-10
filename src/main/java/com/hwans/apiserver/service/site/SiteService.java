package com.hwans.apiserver.service.site;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.admin.AdminReportDto;
import com.hwans.apiserver.dto.common.PageDto;
import com.hwans.apiserver.dto.site.AnnouncementDto;
import com.hwans.apiserver.dto.site.ReportRequestDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.admin.Report;
import com.hwans.apiserver.entity.admin.SiteSetting;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.admin.ReportRepository;
import com.hwans.apiserver.repository.admin.SiteSettingRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.service.admin.AdminAuditService;
import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 사이트 운영 기능 서비스: 화면 맨 위 공지 배너와 글·댓글 신고.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SiteService {
    private static final String ANNOUNCEMENT_KEY = "announcement";
    private static final int SUMMARY_MAX = 200;

    private final SiteSettingRepository siteSettingRepository;
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AccountRepository accountRepository;
    private final BlogService blogService;
    private final AdminAuditService adminAuditService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ───────────── 공지 ─────────────

    /**
     * 저장된 공지를 그대로 돌려준다. (관리자 화면용. 꺼져 있어도 내용이 보인다)
     */
    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncement() {
        return siteSettingRepository.findById(ANNOUNCEMENT_KEY)
                .map(SiteSetting::getValue)
                .map(this::parseAnnouncement)
                .orElseGet(() -> AnnouncementDto.builder().enabled(false).level("info").build());
    }

    /**
     * 방문자에게 보여줄 공지. 꺼져 있거나 내용이 없으면 enabled=false 만 돌려준다.
     */
    @Transactional(readOnly = true)
    public AnnouncementDto getPublicAnnouncement() {
        var announcement = getAnnouncement();
        if (!announcement.isEnabled() || announcement.getMessage() == null || announcement.getMessage().isBlank()) {
            return AnnouncementDto.builder().enabled(false).build();
        }
        return announcement;
    }

    @Transactional
    public AnnouncementDto setAnnouncement(AnnouncementDto request) {
        var message = request.getMessage() == null ? "" : request.getMessage().strip();
        var linkUrl = request.getLinkUrl() == null || request.getLinkUrl().isBlank() ? null : request.getLinkUrl().strip();
        var announcement = AnnouncementDto.builder()
                .enabled(request.isEnabled() && !message.isEmpty())
                .message(message)
                .level(request.getLevel() == null ? "info" : request.getLevel())
                .linkUrl(linkUrl)
                .updatedAt(LocalDateTime.now())
                .build();
        String value;
        try {
            value = objectMapper.writeValueAsString(announcement);
        } catch (Exception e) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        var setting = siteSettingRepository.findById(ANNOUNCEMENT_KEY)
                .orElseGet(() -> SiteSetting.builder().key(ANNOUNCEMENT_KEY).build());
        setting.setValue(value);
        siteSettingRepository.save(setting);
        adminAuditService.record("ANNOUNCEMENT_UPDATE", "SITE", ANNOUNCEMENT_KEY,
                (announcement.isEnabled() ? "공지 켬: " : "공지 끔: ") + message);
        return announcement;
    }

    private AnnouncementDto parseAnnouncement(String value) {
        try {
            return objectMapper.readValue(value, AnnouncementDto.class);
        } catch (Exception e) {
            log.warn("stored announcement is not readable; treating as none");
            return AnnouncementDto.builder().enabled(false).level("info").build();
        }
    }

    // ───────────── 신고 ─────────────

    /**
     * 글·댓글 신고를 접수한다. 같은 사람이(같은 IP) 같은 대상을 이미 신고해 처리 대기 중이면 다시 만들지 않는다.
     */
    @Transactional
    public void createReport(ReportRequestDto request, String reporterEmail, String reporterIp) {
        var targetId = request.getTargetId().toString();
        String summary;
        String path;
        Account targetAuthor;
        if (request.getTargetType() == Report.TargetType.POST) {
            var post = postRepository.findById(request.getTargetId())
                    .filter(x -> !x.isDeleted())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            summary = post.getTitle();
            path = pathOf(post);
            targetAuthor = post.getAuthor();
        } else {
            var comment = commentRepository.findByIdAndDeletedIsFalse(request.getTargetId())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            summary = comment.getContent();
            path = pathOf(comment.getPost());
            targetAuthor = comment.getAuthor();
        }
        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterIpAndStatus(request.getTargetType(), targetId, reporterIp, Report.Status.OPEN)) {
            return;
        }
        var reporter = Optional.ofNullable(reporterEmail).flatMap(accountRepository::findByEmailAndDeletedIsFalse);
        var detail = request.getDetail() == null || request.getDetail().isBlank() ? null : request.getDetail().strip();
        reportRepository.save(Report.builder()
                .targetType(request.getTargetType())
                .targetId(targetId)
                .targetSummary(truncate(summary, SUMMARY_MAX))
                .targetPath(path)
                .targetAuthorEmail(targetAuthor != null && !targetAuthor.isGuest() ? targetAuthor.getEmail() : null)
                .reason(request.getReason())
                .detail(detail)
                .reporterName(reporter.map(Account::getName).orElse(null))
                .reporterEmail(reporter.map(Account::getEmail).orElse(null))
                .reporterIp(reporterIp)
                .status(Report.Status.OPEN)
                .build());
    }

    @Transactional(readOnly = true)
    public PageDto<AdminReportDto> getReports(Optional<Report.Status> status, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var found = status.isPresent() ? reportRepository.findAllByStatus(status.get(), pageable) : reportRepository.findAll(pageable);
        return PageDto.of(found, this::toAdminReportDto);
    }

    /**
     * 신고를 처리한다. deleteTarget 이면 신고된 글·댓글을 지우고, 같은 대상에 걸린 다른 대기 중 신고도 함께 처리 완료로 바꾼다.
     */
    @Transactional
    public AdminReportDto handleReport(UUID reportId, Report.Status status, boolean deleteTarget) {
        var report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        var handler = adminAuditService.currentEmail();
        var label = (report.getTargetType() == Report.TargetType.POST ? "글" : "댓글") + ": " + report.getTargetSummary();
        if (deleteTarget && status == Report.Status.RESOLVED) {
            if (targetExists(report)) {
                var targetUuid = UUID.fromString(report.getTargetId());
                if (report.getTargetType() == Report.TargetType.POST) {
                    postRepository.findById(targetUuid).ifPresent(Post::setDeleted);
                } else {
                    blogService.deleteComment(targetUuid);
                }
            }
            reportRepository
                    .findAllByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), Report.Status.OPEN)
                    .forEach(other -> other.handle(Report.Status.RESOLVED, handler));
            report.handle(Report.Status.RESOLVED, handler);
            adminAuditService.record("REPORT_DELETE_TARGET", "REPORT", report.getId(), "신고 처리(대상 삭제) — " + label);
        } else {
            report.handle(status, handler);
            var action = switch (status) {
                case RESOLVED -> "신고 처리 완료";
                case DISMISSED -> "신고 기각";
                case OPEN -> "신고 다시 열기";
            };
            adminAuditService.record("REPORT_" + status.name(), "REPORT", report.getId(), action + " — " + label);
        }
        return toAdminReportDto(report);
    }

    private boolean targetExists(Report report) {
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(report.getTargetId());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (report.getTargetType() == Report.TargetType.POST) {
            return postRepository.findById(targetUuid).filter(x -> !x.isDeleted()).isPresent();
        }
        return commentRepository.findByIdAndDeletedIsFalse(targetUuid).isPresent();
    }

    private AdminReportDto toAdminReportDto(Report report) {
        return AdminReportDto.builder()
                .id(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .targetSummary(report.getTargetSummary())
                .targetPath(report.getTargetPath())
                .targetExists(targetExists(report))
                .reason(report.getReason())
                .detail(report.getDetail())
                .reporterName(report.getReporterName())
                .reporterEmail(report.getReporterEmail())
                .reporterIp(report.getReporterIp())
                .status(report.getStatus())
                .handledBy(report.getHandledBy())
                .handledAt(report.getHandledAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    // 글 화면 경로 (블로그 Id 는 '@' 를 포함한다)
    private String pathOf(Post post) {
        if (post == null || post.getAuthor() == null) {
            return null;
        }
        return "/" + post.getAuthor().getBlogId() + "/posts/" + post.getPostUrl();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        var oneLine = value.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max);
    }
}
