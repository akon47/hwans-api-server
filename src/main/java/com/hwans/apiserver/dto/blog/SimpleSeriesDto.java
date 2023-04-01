package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.support.annotation.SeriesUrl;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 시리즈 리스트 조회용 Dto
 */
@Getter
@Builder
@ApiModel(description = "시리즈 리스트 조회용 Dto")
public class SimpleSeriesDto implements Serializable {
    @ApiModelProperty(value = "시리즈 URL", required = true, example = "my-first-series")
    @Length(max = 320)
    @SeriesUrl
    String seriesUrl;
    @ApiModelProperty(value = "제목", required = true, example = "제목입니다.")
    @NotBlank
    @Length(max = 2000)
    String title;
    @ApiModelProperty(value = "게시글 수")
    int postCount;
}
