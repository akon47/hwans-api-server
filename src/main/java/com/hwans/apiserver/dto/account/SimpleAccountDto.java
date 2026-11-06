package com.hwans.apiserver.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 간단한 계정 정보 Dto
 */
@Getter
@Builder
@Schema(description = "간단한 계정 정보 Dto")
public class SimpleAccountDto implements Serializable {
    @Schema(description = "계정 사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "김환")
    @NotBlank
    String name;
    @Schema(description = "블로그 Id", example = "kim-hwan")
    String blogId;
    @Schema(description = "프로필 이미지 URL", example = "/attachments/file-id")
    String profileImageUrl;
    @Schema(description = "비회원인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean guest;
}
