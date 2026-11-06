package com.hwans.apiserver.dto.token;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 방금 발급된 개인 액세스 토큰 Dto. 토큰 원문은 이 응답에서 한 번만 볼 수 있다.
 */
@Getter
@Builder
@ApiModel(description = "방금 발급된 개인 액세스 토큰 Dto (토큰 원문은 한 번만 표시)")
public class IssuedAccessTokenDto implements Serializable {
    @ApiModelProperty(value = "토큰 원문 (Authorization: Bearer <token>)", required = true)
    String token;
    @ApiModelProperty(value = "토큰 정보", required = true)
    AccessTokenDto accessToken;
}
