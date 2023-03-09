package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.blog.PostDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.entity.blog.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 게시글 엔티티와 게시글 데이터 모델 사이의 변환을 제공한다.
 */
@Mapper(componentModel = "spring")
public interface PostMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "thumbnailImage", ignore = true)
    @Mapping(target = "hits", ignore = true)
    @Mapping(target = "postSeries", ignore = true)
    Post PostRequestDtoToEntity(PostRequestDto postRequestDto);

    PostDto EntityToPostDto(Post post);

    SimplePostDto EntityToSimplePostDto(Post post);
}
