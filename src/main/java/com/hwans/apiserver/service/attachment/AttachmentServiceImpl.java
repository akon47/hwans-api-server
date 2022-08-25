package com.hwans.apiserver.service.attachment;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AccountRepository accountRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileMapper fileMapper;

    @Value("${attachments.path}")
    private String attachmentsBasePath;

    @Override
    @Transactional
    public FileDto saveImageFile(UUID uploaderAccountId, MultipartFile multipartFile) {
        if (!multipartFile.getContentType().startsWith("image")) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }

        var uploaderAccount = accountRepository
                .findById(uploaderAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var savePath = Paths.get(getAttachmentDirectoryPath() + File.separator + UUID.randomUUID());
        try {
            multipartFile.transferTo(savePath);
        } catch (IOException e) {
            throw new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        File savedFile = savePath.toFile();
        var attachment = attachmentRepository
                .save(new Attachment(uploaderAccount, savedFile, multipartFile.getOriginalFilename(), multipartFile.getContentType()));
        return fileMapper.EntityToFileDto(attachment);
    }

    @Override
    @Transactional
    public FileDto saveImageFileFromUrl(UUID uploaderAccountId, String imageUrl) {
        try {
            var uploaderAccount = accountRepository
                    .findById(uploaderAccountId)
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));

            var url = new URL(imageUrl);
            var connection = url.openConnection();
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();
            if (!contentType.startsWith("image") || contentLength < 0 || contentLength > 1024 * 1024 * 20) {
                throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
            }
            var savePath = Paths.get(getAttachmentDirectoryPath() + File.separator + UUID.randomUUID());
            Files.copy(connection.getInputStream(), savePath);
            File savedFile = savePath.toFile();
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

    private String getAttachmentDirectoryPath() {
        var directoryName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd"));
        var attachmentDirectory = new File(attachmentsBasePath, directoryName);

        if (!attachmentDirectory.exists()) {
            attachmentDirectory.mkdirs();
        }
        return attachmentDirectory.getAbsolutePath();
    }
}
