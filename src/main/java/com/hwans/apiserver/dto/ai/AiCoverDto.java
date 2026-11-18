package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 만든 커버 이미지 Dto (첨부 파일로 저장됨)
 */
@Getter
@Builder
@Schema(description = "만든 커버 이미지 Dto")
public class AiCoverDto implements Serializable {
    @Schema(description = "첨부 파일 Id")
    String fileId;
    @Schema(description = "파일 URL (파일 업로드 API 가 주는 것과 같은 형식)", example = "/attachments/3f2c.../image.png")
    String url;
}
