package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.attachment.FileDto;
import com.hwans.apiserver.entity.attachment.Attachment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMapper {
    FileDto EntityToFileDto(Attachment attachment);
}
