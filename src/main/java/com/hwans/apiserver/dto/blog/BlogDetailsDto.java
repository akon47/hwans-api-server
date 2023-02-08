package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 블로그 상세 Dto
 */
@Getter
@Builder
@ApiModel(description = "블로그 상세 Dto")
public class BlogDetailsDto implements Serializable {
    @ApiModelProperty(value = "블로그 주인", required = true)
    @NotNull
    AccountDto owner;
    @ApiModelProperty(value = "전체 게시글 수")
    int postCount;

}
