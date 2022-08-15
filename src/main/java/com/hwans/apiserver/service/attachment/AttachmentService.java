package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.dto.attachment.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public interface AttachmentService {
    FileDto saveImageFile(UUID uploaderAccountId, MultipartFile multipartFile);
    FileDto getFile(UUID fileId);
    File getLocalFile(UUID fileId);
    AttachmentResource getFileAsResource(UUID fileId);
}
