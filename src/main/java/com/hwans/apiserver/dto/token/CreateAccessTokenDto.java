package com.hwans.apiserver.dto.token;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 개인 액세스 토큰 발급 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "개인 액세스 토큰 발급 요청 Dto")
public class CreateAccessTokenDto implements Serializable {
    @ApiModelProperty(value = "토큰 이름 (어디에 쓰는지)", required = true, example = "Claude Code - 블로그 글쓰기")
    @NotBlank
    @Length(max = 100)
    String name;
    @ApiModelProperty(value = "권한 목록 (posts, social)", required = true, example = "[\"posts\"]")
    @NotEmpty
    List<String> scopes;
    @ApiModelProperty(value = "만료까지 일수 (null 또는 0 이면 무기한, 최대 3650)", example = "30")
    Integer ttlDays;
}
