package com.hwans.apiserver.service.attachment;

import com.hwans.apiserver.dto.attachment.FileDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 첨부파일 서비스 인터페이스
 */
public interface AttachmentService {
    /**
     * @param uploaderAccountId 업로더 계정 Id
     * @param multipartFile 업로드를 원하는 파일의 MultipartFile
     * @return 업로드 된 파일의 데이터 모델
     */
    FileDto saveFile(UUID uploaderAccountId, MultipartFile multipartFile);

    /**
     * URL을 이용하여 파일을 업로드 한다.
     *
     * @param uploaderAccountId 업로더 계정 Id
     * @param fileUrl 업로드를 원하는 파일의 Url
     * @return 업로드 된 파일의 데이터 모델
     */
    FileDto saveFileFromUrl(UUID uploaderAccountId, String fileUrl);

    /**
     * 파일 Id로 파일 데이터 모델을 얻는다.
     *
     * @param fileId 파일 Id
     * @return 파일 데이터 모델
     */
    FileDto getFile(UUID fileId);

    /**
     * 파일 Id로 실제 로컬에 저장되어 있는 파일을 얻는다.
     *
     * @param fileId 파일 Id
     * @return 실제 로컬에 저장되어 있는 파일
     */
    File getLocalFile(UUID fileId);

    /**
     * 파일 Id로 해당 파일에 대한 첨부파일 리소르를 얻는다.
     *
     * @param fileId 파일 Id
     * @return 해당 파일에 대한 첨부파일 리소스
     */
    AttachmentResource getFileAsResource(UUID fileId);

    /**
     * 파일 Id와 파일의 기본 이름과 확장자를 이용하여 정확히 일치하는 파일에 대한 첨부파일 리소르를 얻는다.
     *
     * @param fileId 파일 Id
     * @param fileTypeWithExt 파일의 기본 이름과 확장자
     * @return 해당 파일에 대한 첨부파일 리소스
     */
    AttachmentResource getFileAsResource(UUID fileId, String fileTypeWithExt);
}
