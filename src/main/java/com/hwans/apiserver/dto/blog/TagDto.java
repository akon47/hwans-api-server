package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "태그 Dto")
public class TagDto implements Serializable {
    @ApiModelProperty(value = "태그 이름", required = true, example = "C#")
    @NotBlank
    String name;
}
