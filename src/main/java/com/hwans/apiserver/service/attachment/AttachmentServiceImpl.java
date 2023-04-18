package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.attachment.FileDto;
import com.hwans.apiserver.entity.attachment.Attachment;
import com.hwans.apiserver.mapper.FileMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 첨부파일 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AccountRepository accountRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileMapper fileMapper;

    private static final String BAD_ATTACHMENT_CONTENT_TYPE = "업로드가 불가능한 파일 형식입니다.";

    @Value("${attachments.path}")
    private String attachmentsBasePath;

    /**
     * 해당 ContentType이 저장이 가능한 파일 형식인지 여부를 반환한다.
     *
     * @param contentType 확인할 ContentType
     * @return 저장 가능한 파일 형식인지 여부
     */
    private boolean isSavableContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.matches("(^video\\/(mp4|webm))|(^image\\/.*)|(^application\\/(pdf|x-msdownload|x-zip-compressed))");
    }

    @Override
    @Transactional
    public FileDto saveFile(UUID uploaderAccountId, MultipartFile multipartFile) {
        var contentType = multipartFile.getContentType();
        if (!isSavableContentType(contentType)) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST, BAD_ATTACHMENT_CONTENT_TYPE);
        }
        var uploaderAccount = accountRepository
                .findByIdAndDeletedIsFalse(uploaderAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));
        var savePath = Paths.get(getAttachmentDirectoryPath() + File.separator + UUID.randomUUID());
        try {
            multipartFile.transferTo(savePath);
        } catch (IOException e) {
            throw new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        var savedFile = savePath.toFile();
        var attachment = attachmentRepository
                .save(new Attachment(uploaderAccount, savedFile, multipartFile.getOriginalFilename(), multipartFile.getContentType()));
        return fileMapper.EntityToFileDto(attachment);
    }

    @Override
    @Transactional
    public FileDto saveFileFromUrl(UUID uploaderAccountId, String fileUrl) {
        try {
            var uploaderAccount = accountRepository
                    .findByIdAndDeletedIsFalse(uploaderAccountId)
                    .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));
            var url = new URL(fileUrl);
            var connection = url.openConnection();
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();
            if (!isSavableContentType(contentType) || contentLength <= 0 || contentLength > Constants.MAX_ATTACHMENT_FILE_SIZE) {
                throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST, BAD_ATTACHMENT_CONTENT_TYPE);
            }
            var savePath = Paths.get(getAttachmentDirectoryPath() + File.separator + UUID.randomUUID());
            Files.copy(connection.getInputStream(), savePath);
            var savedFile = savePath.toFile();
            var attachment = attachmentRepository
                    .save(new Attachment(uploaderAccount, savedFile, null, contentType));
            return fileMapper.EntityToFileDto(attachment);
        } catch (IOException e) {
            throw new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    public FileDto getFile(UUID fileId) {
        var foundFile = attachmentRepository
                .findById(fileId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        return fileMapper.EntityToFileDto(foundFile);
    }

    @Override
    public File getLocalFile(UUID fileId) {
        var foundFile = attachmentRepository
                .findById(fileId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        return new File(foundFile.getLocalFilePath());
    }

    @Override
    public AttachmentResource getFileAsResource(UUID fileId) {
        var foundFile = attachmentRepository
                .findById(fileId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        try {
            return new AttachmentResource(foundFile);
        } catch (IOException e) {
            throw new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public AttachmentResource getFileAsResource(UUID fileId, String fileTypeWithExt) {
        var foundFile = attachmentRepository
                .findById(fileId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        if (foundFile.getFileTypeWithExt().equals(fileTypeWithExt) == false) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND);
        }

        try {
            return new AttachmentResource(foundFile);
        } catch (IOException e) {
            throw new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 첨부파일의 기본 디렉토리를 반환한다.
     * 
     * @return 디렉토리 경로
     */
    private String getAttachmentDirectoryPath() {
        var directoryName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        var attachmentDirectory = new File(attachmentsBasePath, directoryName);

        if (!attachmentDirectory.exists()) {
            attachmentDirectory.mkdirs();
        }
        return attachmentDirectory.getAbsolutePath();
    }
}
