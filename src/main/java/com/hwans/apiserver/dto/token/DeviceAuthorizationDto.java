package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.entity.token.DeviceAuthorization;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기기 로그인 요청 Dto (사람이 승인 화면에서 확인하는 내용)
 */
@Getter
@Builder
@ApiModel(description = "기기 로그인 요청 Dto")
public class DeviceAuthorizationDto implements Serializable {
    @ApiModelProperty(value = "사용자 코드", required = true, example = "BCDF-GHJK")
    String userCode;
    @ApiModelProperty(value = "요청한 클라이언트 이름", required = true, example = "Claude Code - 블로그 글쓰기")
    String clientName;
    @ApiModelProperty(value = "요청한 권한 목록", required = true)
    List<String> scopes;
    @ApiModelProperty(value = "요청한 IP")
    String requestedIp;
    @ApiModelProperty(value = "요청한 User-Agent")
    String requestedUa;
    @ApiModelProperty(value = "상태", required = true)
    DeviceAuthorization.Status status;
    @ApiModelProperty(value = "요청 시각", required = true)
    LocalDateTime createdAt;
    @ApiModelProperty(value = "코드 만료 시각", required = true)
    LocalDateTime expiresAt;
}
