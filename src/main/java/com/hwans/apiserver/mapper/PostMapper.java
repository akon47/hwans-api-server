package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.blog.PostDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.entity.blog.Post;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PostMapper {
    Post PostRequestDtoToEntity(PostRequestDto postRequestDto);

    PostDto PostEntityToPostDto(Post post);
}
