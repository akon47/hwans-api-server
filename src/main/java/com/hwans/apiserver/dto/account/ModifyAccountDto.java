package com.hwans.apiserver.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 사용자 정보 수정 Dto
 */
@Getter
@Builder
@Schema(description = "사용자 정보 수정 Dto")
public class ModifyAccountDto implements Serializable {
    @Schema(description = "계정 사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
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
}
