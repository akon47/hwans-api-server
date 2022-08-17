package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.blog.CommentRequestDto;
import com.hwans.apiserver.entity.blog.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Comment toEntity(CommentRequestDto commentRequestDto);
}
