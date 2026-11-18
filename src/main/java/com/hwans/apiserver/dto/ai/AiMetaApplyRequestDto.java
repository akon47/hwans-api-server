package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 고른 제목·요약·태그 적용 요청 Dto. 항목마다 온 필드만 바꾼다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제목·요약·태그 적용 요청 Dto")
public class AiMetaApplyRequestDto implements Serializable {
    @Schema(description = "적용할 글 (1~100개)", requiredMode = Schema.RequiredMode.REQUIRED)
    List<Item> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "글 하나에 적용할 값 (null 이거나 없는 필드는 그대로)")
    public static class Item implements Serializable {
        @Schema(description = "내 글 주소", requiredMode = Schema.RequiredMode.REQUIRED)
        String postUrl;
        @Schema(description = "새 제목 (선택, 2000자 이내)")
        String title;
        @Schema(description = "새 요약 (선택, 255자 이내)")
        String summary;
        @Schema(description = "새 태그 (선택, 앞뒤 공백·중복 제거 후 10개 이내)")
        List<String> tags;
    }
}
