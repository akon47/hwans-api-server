package com.hwans.apiserver.dto.authentication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Builder
@Accessors(chain = true)
@ApiModel(description = "계정 인증 토큰 Dto")
public class TokenDto implements Serializable {
    @ApiModelProperty(value = "인증 토큰 값", required = true)
    String accessToken;
    @ApiModelProperty(value = "인증 토큰 만료 시간", required = true)
    Long accessTokenExpiresIn;
}
