package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 지금 요청에 쓰인 액세스 토큰과 그 주인 계정 정보 Dto (에이전트의 "나는 누구인가")
 */
@Getter
@Builder
@Schema(description = "현재 액세스 토큰과 계정 정보 Dto")
public class CurrentAccessTokenDto implements Serializable {
    @Schema(description = "토큰 주인 계정", requiredMode = Schema.RequiredMode.REQUIRED)
    AccountDto account;
    @Schema(description = "토큰 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    AccessTokenDto token;
    @Schema(description = "블로그 웹 주소", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://hwanstory.kr")
    String webBaseUrl;
    @Schema(description = "API 서버 주소", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.hwanstory.kr")
    String apiBaseUrl;
}
