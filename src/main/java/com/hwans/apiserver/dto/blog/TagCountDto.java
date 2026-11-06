package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 게시글 태그 개수 Dto
 */
@Getter
@Builder
@Schema(description = "태그 개수 Dto")
@NoArgsConstructor
@AllArgsConstructor
public class TagCountDto implements Serializable {
    @Schema(description = "태그 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "C#")
    @NotBlank
    String name;
    @Schema(description = "해당 태그를 가지고 있는 게시글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    int count;
}
