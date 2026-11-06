package com.hwans.apiserver.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 사용자 정보 Dto
 */
@Getter
@Builder
@Schema(description = "사용자 정보 Dto")
public class AccountDto implements Serializable {
    @Schema(description = "계정 사용자 이메일", requiredMode = Schema.RequiredMode.REQUIRED, example = "akon47@naver.com")
    @NotBlank
    @Length(max = 320)
    @Email
    String email;
    @Schema(description = "계정 사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @Schema(description = "블로그 Id", requiredMode = Schema.RequiredMode.REQUIRED, example = "@kim-hwan")
    @NotBlank
    @Length(max = 64)
    String blogId;
    @Schema(description = "프로필 이미지 URL", example = "/file-id")
    String profileImageUrl;
    @Schema(description = "간단한 자기소개", example = "안녕하세요, 반갑습니다.")
    @Length(max = 255)
    String biography;
    @Schema(description = "회사", example = "google")
    @Length(max = 64)
    String company;
    @Schema(description = "위치", example = "seoul")
    @Length(max = 64)
    String location;
    @Schema(description = "홈페이지", example = "https://kimhwan.kr")
    @Length(max = 255)
    String homepage;
    @Schema(description = "비회원인지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean guest;
}
