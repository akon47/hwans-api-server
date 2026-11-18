package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 회원의 문체 프로필 Dto
 */
@Getter
@Builder
@Schema(description = "문체 프로필 Dto")
public class AiStyleDto implements Serializable {
    @Schema(description = "분석된 문체가 있는지", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean analyzed;
    @Schema(description = "분석한 시각 (yyyy-MM-dd'T'HH:mm:ss, 없으면 null)", example = "2026-11-18T12:00:00")
    String analyzedAt;
    @Schema(description = "분석에 쓴 글 수", example = "8")
    int postCount;
    @Schema(description = "문체 설명 (한국어, 2000자 이내, 없으면 null)")
    String profile;
}
