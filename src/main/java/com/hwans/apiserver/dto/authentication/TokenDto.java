package com.hwans.apiserver.dto.authentication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 계정 인증 토큰 Dto
 */
@Getter
@Builder
@ApiModel(description = "계정 인증 토큰 Dto")
public class TokenDto implements Serializable {
    @ApiModelProperty(value = "인증 토큰 값", required = true)
    @NotBlank
    String accessToken;
    @ApiModelProperty(value = "인증 토큰 만료 시간", required = true)
    Long accessTokenExpiresIn;
    @ApiModelProperty(value = "갱신 토큰 값", required = true)
    @NotBlank
    String refreshToken;
    @ApiModelProperty(value = "갱신 토큰 만료 시간", required = true)
    Long refreshTokenExpiresIn;
}