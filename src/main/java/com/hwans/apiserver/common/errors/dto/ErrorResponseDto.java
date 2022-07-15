package com.hwans.apiserver.common.errors.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@Builder
public class ErrorResponseDto implements Serializable {
    private String message;
}
