package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 지금 요청에 쓰인 액세스 토큰과 그 주인 계정 정보 Dto (에이전트의 "나는 누구인가")
 */
@Getter
@Builder
@ApiModel(description = "현재 액세스 토큰과 계정 정보 Dto")
public class CurrentAccessTokenDto implements Serializable {
    @ApiModelProperty(value = "토큰 주인 계정", required = true)
    AccountDto account;
    @ApiModelProperty(value = "토큰 정보", required = true)
    AccessTokenDto token;
    @ApiModelProperty(value = "블로그 웹 주소", required = true, example = "https://hwanstory.kr")
    String webBaseUrl;
    @ApiModelProperty(value = "API 서버 주소", required = true, example = "https://api.hwanstory.kr")
    String apiBaseUrl;
}
