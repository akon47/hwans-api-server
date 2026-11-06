package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.io.Serializable;

/**
 * 관리자 권한 부여/회수 요청 Dto
 */
@Getter
@Schema(description = "관리자 권한 부여/회수 요청 Dto")
public class AdminRoleRequestDto implements Serializable {
    @Schema(description = "true 면 관리자 권한을 주고, false 면 뺀다", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean admin;
}
