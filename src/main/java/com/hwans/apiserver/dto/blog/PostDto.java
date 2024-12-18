package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.blog.OpenType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 Dto
 */
@Getter
@Builder
@ApiModel(description = "게시글 Dto")
public class PostDto implements Serializable {
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
    @ApiModelProperty(value = "내용", required = true, example = "게시글 내용입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "게시글 공개 유형", required = true)
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
    List<TagDto> tags;
    @ApiModelProperty(value = "댓글")
    List<SimpleCommentDto> comments;
    @ApiModelProperty(value = "좋아요 수")
    int likeCount;
    @ApiModelProperty(value = "조회수")
    @With
    int hits;
    @ApiModelProperty(value = "시리즈 Url")
    String seriesUrl;
}
