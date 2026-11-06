package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 관리자용 계정 정보 Dto
 */
@Getter
@Builder
@Schema(description = "관리자용 계정 정보 Dto")
public class AdminAccountDto implements Serializable {
    @Schema(description = "계정 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "이메일", requiredMode = Schema.RequiredMode.REQUIRED)
    String email;
    @Schema(description = "이름", requiredMode = Schema.RequiredMode.REQUIRED)
    String name;
    @Schema(description = "블로그 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    String blogId;
    @Schema(description = "프로필 이미지 URL")
    String profileImageUrl;
    @Schema(description = "정지(비활성화) 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean deleted;
    @Schema(description = "보유 역할 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<String> roles;
    @Schema(description = "가입 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
}
