package com.hwans.apiserver.common.errors.exception;

import com.hwans.apiserver.common.errors.errorcode.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class RestApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private String message;
}
