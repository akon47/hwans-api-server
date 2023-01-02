package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.dto.attachment.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 첨부파일 서비스 인터페이스
 */
public interface AttachmentService {
    FileDto saveFile(UUID uploaderAccountId, MultipartFile multipartFile);
    FileDto saveFileFromUrl(UUID uploaderAccountId, String fileUrl);
    FileDto getFile(UUID fileId);
    File getLocalFile(UUID fileId);
    AttachmentResource getFileAsResource(UUID fileId);
    AttachmentResource getFileAsResource(UUID fileId, String fileTypeWithExt);
}
