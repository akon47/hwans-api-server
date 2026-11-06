package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 좋아요 Dto
 */
@Getter
@Builder
@Schema(description = "좋아요 Dto")
public class LikeDto implements Serializable {
    @Schema(description = "좋아요를 한 게시글", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    PostDto post;
    @Schema(description = "좋아요를 한 사용자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    AccountDto account;
}
