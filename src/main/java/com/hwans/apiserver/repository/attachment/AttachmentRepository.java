package com.hwans.apiserver.repository.attachment;

import com.hwans.apiserver.entity.attachment.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 첨부파일 Repository
 */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    void deleteAllByAccountId(UUID accountId);

    // 관리자용: 첨부파일 총 용량
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(x.fileSize), 0) from Attachment as x")
    long sumFileSize();
}
