package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.blog.OpenType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 리스트 조회용 Dto
 */
@Getter
@Builder
@ApiModel(description = "게시글 리스트 조회용 Dto")
public class SimplePostDto implements Serializable {
    @ApiModelProperty(value = "게시글 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "게시글 URL", required = true, example = "my-first-post")
    @NotBlank
    String postUrl;
    @ApiModelProperty(value = "제목", required = true, example = "제목입니다.")
    @NotBlank
    String title;
    @ApiModelProperty(value = "요약 내용", required = true, example = "요약 내용입니다.")
    @NotBlank
    String summary;
    @ApiModelProperty(value = "게시글 공개 유형", required = true, example = "PUBLIC")
    @NotNull
    OpenType openType;
    @ApiModelProperty(value = "썸네일 이미지 URL", example = "/file-id")
    @NotBlank
    String thumbnailImageUrl;
    @ApiModelProperty(value = "글쓴이", required = true)
    @NotBlank
    SimpleAccountDto author;
    @ApiModelProperty(value = "작성 시간", required = true)
    @NotNull
    LocalDateTime createdAt;
    @ApiModelProperty(value = "마지막 수정 시간", required = true)
    @NotNull
    LocalDateTime lastModifiedAt;
    @ApiModelProperty(value = "태그")
    Set<TagDto> tags;
    @ApiModelProperty(value = "댓글 수")
    int commentCount;
    @ApiModelProperty(value = "좋아요 수")
    int likeCount;
    @ApiModelProperty(value = "조회수")
    int hits;
}
