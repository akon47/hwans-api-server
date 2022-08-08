package com.hwans.apiserver.dto.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@ApiModel(description = "커서 페이징 조회 응답을 위한 Dto")
public class SliceDto<T extends Serializable> {
    @ApiModelProperty(value = "조회된 데이터")
    List<T> data;
    @ApiModelProperty(value = "조회된 데이터 수")
    Long size;
    @ApiModelProperty(value = "조회된 데이터가 없는지 여부")
    boolean empty;
    @ApiModelProperty(value = "첫 페이지 여부")
    boolean first;
    @ApiModelProperty(value = "마지막 페이지 여부")
    boolean last;
    @ApiModelProperty(value = "다음 페이지 조회를 위한 커서 Id")
    String nextCursorId;
}
