package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.entity.blog.OpenType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.io.Serializable;

/**
 * 관리자가 글의 공개 유형을 바꾸는 요청 Dto
 */
@Getter
@Schema(description = "관리자 글 공개 유형 변경 요청 Dto")
public class AdminOpenTypeRequestDto implements Serializable {
    @NotNull
    @Schema(description = "바꿀 공개 유형", requiredMode = Schema.RequiredMode.REQUIRED)
    OpenType openType;
}
