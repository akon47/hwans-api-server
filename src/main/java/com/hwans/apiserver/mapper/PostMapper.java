package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.blog.PostDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.entity.blog.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "blogId", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Post PostRequestDtoToEntity(PostRequestDto postRequestDto);

    PostDto EntityToPostDto(Post post);

    SimplePostDto EntityToSimplePostDto(Post post);
}
