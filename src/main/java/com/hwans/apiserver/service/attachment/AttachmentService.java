package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.dto.attachment.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

public interface AttachmentService {
    FileDto saveFile(UUID uploaderAccountId, MultipartFile multipartFile);
    FileDto saveFileFromUrl(UUID uploaderAccountId, String fileUrl);
    FileDto getFile(UUID fileId);
    File getLocalFile(UUID fileId);
    AttachmentResource getFileAsResource(UUID fileId);
}
