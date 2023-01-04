package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.attachment.FileDto;
import com.hwans.apiserver.entity.attachment.Attachment;
import org.mapstruct.Mapper;

/**
 * 첨부파일 엔티티와 첨부파일 데이터 모델 사이의 변환을 제공한다.
 */
@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDto EntityToFileDto(Attachment attachment);
}
