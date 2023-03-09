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

/**
 * 시리즈 Dto
 */
@Getter
@Builder
@ApiModel(description = "시리즈 Dto")
public class SeriesDto implements Serializable {
    @ApiModelProperty(value = "시리즈 URL", required = true, example = "my-first-series")
    @Length(max = 320)
    @PostUrl
    String seriesUrl;
    @ApiModelProperty(value = "제목", required = true, example = "제목입니다.")
    @NotBlank
    @Length(max = 2000)
    String title;
    @ApiModelProperty(value = "게시글 목록")
    Set<PostDto> posts;
}
