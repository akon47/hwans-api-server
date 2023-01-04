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
 * 게시글 태그 Dto
 */
@Getter
@Builder
@ApiModel(description = "태그 Dto")
@NoArgsConstructor
@AllArgsConstructor
public class TagDto implements Serializable {
    @ApiModelProperty(value = "태그 이름", required = true, example = "C#")
    @NotBlank
    String name;
}
