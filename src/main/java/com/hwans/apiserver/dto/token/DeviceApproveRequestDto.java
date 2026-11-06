package com.hwans.apiserver.dto.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 기기 로그인 승인 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "기기 로그인 승인 요청 Dto")
public class DeviceApproveRequestDto implements Serializable {
    @Schema(description = "발급할 토큰의 만료까지 일수 (null 이면 기본값, 0 이면 무기한, 최대 3650)", example = "30")
    Integer ttlDays;
}
