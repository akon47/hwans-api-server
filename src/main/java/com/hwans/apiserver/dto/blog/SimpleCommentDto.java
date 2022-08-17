package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@ApiModel(description = "댓글 리스트 조회용 Dto")
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCommentDto implements Serializable {
    @ApiModelProperty(value = "댓글 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "부모 댓글 Id", required = true)
    UUID parentId;
    @ApiModelProperty(value = "대댓글 개수", required = true)
    int childrenCount;
    @ApiModelProperty(value = "댓글을 단 사용자", required = true)
    @NotNull
    SimpleAccountDto account;
    @ApiModelProperty(value = "작성 시간", required = true)
    @NotBlank
    LocalDateTime createdAt;
}
