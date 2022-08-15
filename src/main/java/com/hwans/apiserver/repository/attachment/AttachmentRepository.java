package com.hwans.apiserver.repository.attachment;

import com.hwans.apiserver.entity.attachment.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

}
