package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.support.annotation.PostUrl;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@ApiModel(description = "게시글 작성/수정 Dto")
public class PostRequestDto implements Serializable {
    @ApiModelProperty(value = "게시글 URL", required = true, example = "my-first-post")
    @Length(max = 320)
    @PostUrl
    String postUrl;
    @ApiModelProperty(value = "제목", required = true, example = "제목입니다.")
    @NotBlank
    @Length(max = 2000)
    String title;
    @ApiModelProperty(value = "내용", required = true, example = "게시글 내용입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "요약 내용", example = "요약 내용입니다.")
    @NotBlank
    @Length(max = 255)
    String summary;
    @ApiModelProperty(value = "썸네일 파일 Id")
    UUID thumbnailFileId;
    @ApiModelProperty(value = "태그", required = true)
    Set<TagDto> tags;
}
