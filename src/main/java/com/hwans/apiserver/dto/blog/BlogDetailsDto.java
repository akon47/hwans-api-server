package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 블로그 상세 Dto
 */
@Getter
@Builder
@Schema(description = "블로그 상세 Dto")
public class BlogDetailsDto implements Serializable {
    @Schema(description = "블로그 주인", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    AccountDto owner;
    @Schema(description = "전체 게시글 수")
    int postCount;
    @Schema(description = "팔로워 수")
    long followerCount;
    @Schema(description = "팔로잉 수")
    long followingCount;
    @Schema(description = "게시글에서 사용된 태그들과 해당 태그가 존재하는 게시글의 개수 정보")
    List<TagCountDto> tagCounts;
}
