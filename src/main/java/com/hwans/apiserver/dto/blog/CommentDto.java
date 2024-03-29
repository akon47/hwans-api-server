package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 댓글 Dto
 */
@Getter
@Builder
@ApiModel(description = "댓글 Dto")
public class CommentDto implements Serializable {
    @ApiModelProperty(value = "댓글 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "부모 댓글 Id", required = true)
    UUID parentId;
    @ApiModelProperty(value = "대댓글")
    @NotBlank
    Set<SimpleCommentDto> children;
    @ApiModelProperty(value = "댓글이 달려있는 게시글", required = true)
    @NotBlank
    SimplePostDto post;
    @ApiModelProperty(value = "댓글을 단 사용자", required = true)
    @NotBlank
    AccountDto author;
    @ApiModelProperty(value = "작성 시간", required = true)
    @NotBlank
    LocalDateTime createdAt;
}
