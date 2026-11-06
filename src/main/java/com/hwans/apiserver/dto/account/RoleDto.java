package com.hwans.apiserver.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 사용자 계정 역할 Dto
 */
@Getter
@Builder
@Schema(description = "역할 Dto")
public class RoleDto implements Serializable {
    @Schema(description = "역할 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "ROLE_USER")
    @NotBlank
    String name;
}
