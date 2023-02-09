package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 게시글 태그 개수 Dto
 */
@Getter
@Builder
@ApiModel(description = "태그 개수 Dto")
@NoArgsConstructor
@AllArgsConstructor
public class TagCountDto implements Serializable {
    @ApiModelProperty(value = "태그 이름", required = true, example = "C#")
    @NotBlank
    String name;
    @ApiModelProperty(value = "해당 태그를 가지고 있는 게시글 수", required = true)
    @NotBlank
    int count;
}
