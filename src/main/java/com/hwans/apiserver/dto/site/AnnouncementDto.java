package com.hwans.apiserver.dto.site;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 사이트 공지 Dto (화면 맨 위 배너. 조회와 관리자 저장 요청에 함께 쓴다)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사이트 공지 Dto")
public class AnnouncementDto implements Serializable {
    @Schema(description = "공지를 보여줄지", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean enabled;
    @Schema(description = "공지 내용")
    @Size(max = 300)
    String message;
    @Schema(description = "종류 (info / success / warning)")
    @Pattern(regexp = "info|success|warning")
    String level;
    @Schema(description = "자세히 보기 링크 (사이트 안 경로 또는 https 주소)")
    @Size(max = 500)
    @Pattern(regexp = "^$|^/[^/].*|^https://.+")
    String linkUrl;
    @Schema(description = "마지막으로 고친 시각 (저장 요청에서는 무시된다)")
    LocalDateTime updatedAt;
}
