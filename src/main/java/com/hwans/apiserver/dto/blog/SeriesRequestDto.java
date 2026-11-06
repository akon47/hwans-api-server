package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.support.annotation.SeriesUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 시리즈 생성/수정 Dto
 */
@Getter
@Builder
@Schema(description = "시리즈 생성/수정 Dto")
public class SeriesRequestDto implements Serializable {
    @Schema(description = "시리즈 URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "my-first-series")
    @Length(max = 320)
    @SeriesUrl
    String seriesUrl;
    @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "제목입니다.")
    @NotBlank
    @Length(max = 2000)
    String title;
}
