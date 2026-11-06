package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 개인 액세스 토큰 정보 Dto (토큰 원문은 포함하지 않는다)
 */
@Getter
@Builder
@ApiModel(description = "개인 액세스 토큰 정보 Dto")
public class AccessTokenDto implements Serializable {
    @ApiModelProperty(value = "토큰 레코드 Id (폐기할 때 사용)", required = true)
    UUID id;
    @ApiModelProperty(value = "토큰 공개 Id (토큰 문자열 hsp_<tokenId>_… 의 가운데 부분)", required = true)
    String tokenId;
    @ApiModelProperty(value = "이름", required = true, example = "Claude Code - 블로그 글쓰기")
    String name;
    @ApiModelProperty(value = "권한 목록", required = true, example = "[\"posts\", \"social\"]")
    List<String> scopes;
    @ApiModelProperty(value = "발급 경로", required = true)
    AccessTokenOrigin origin;
    @ApiModelProperty(value = "발급 시각", required = true)
    LocalDateTime createdAt;
    @ApiModelProperty(value = "만료 시각 (null 이면 무기한)")
    LocalDateTime expiresAt;
    @ApiModelProperty(value = "마지막 사용 시각")
    LocalDateTime lastUsedAt;
    @ApiModelProperty(value = "마지막 사용 IP")
    String lastUsedIp;
    @ApiModelProperty(value = "폐기 시각 (null 이면 폐기되지 않음)")
    LocalDateTime revokedAt;
    @ApiModelProperty(value = "만료 여부", required = true)
    boolean expired;
    @ApiModelProperty(value = "사용 가능 여부 (폐기되지 않았고 만료되지 않음)", required = true)
    boolean usable;
}
