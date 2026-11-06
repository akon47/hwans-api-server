package com.hwans.apiserver.dto.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 기기 로그인 시작 응답 Dto (RFC 8628 필드 이름)
 */
@Getter
@Builder
@Schema(description = "기기 로그인 시작 응답 Dto (RFC 8628 필드 이름)")
public class DeviceCodeDto implements Serializable {
    @Schema(description = "에이전트가 보관하는 비밀 코드 (폴링에 사용, 출력하지 말 것)", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("device_code")
    String deviceCode;
    @Schema(description = "사람이 승인 화면에 입력하는 짧은 코드", requiredMode = Schema.RequiredMode.REQUIRED, example = "BCDF-GHJK")
    @JsonProperty("user_code")
    String userCode;
    @Schema(description = "승인 화면 주소", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("verification_uri")
    String verificationUri;
    @Schema(description = "코드가 채워진 승인 화면 주소", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("verification_uri_complete")
    String verificationUriComplete;
    @Schema(description = "코드 유효 시간 (초)", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("expires_in")
    int expiresIn;
    @Schema(description = "폴링 최소 간격 (초)", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("interval")
    int interval;
}
