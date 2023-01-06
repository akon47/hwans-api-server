package com.hwans.apiserver.common.errors.exception;

import com.hwans.apiserver.common.errors.errorcode.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 호출 시 예외가 발생하면 throw 되는 예외 클래스
 */
@Getter
@AllArgsConstructor
public class RestApiException extends RuntimeException {
    /**
     * 에러 코드
     */
    private final ErrorCode errorCode;

    /**
     * 에러 메시지
     */
    private final String message;

    /**
     * 에러 코드로부터 예외를 생성한다.
     *
     * @param errorCode 에러 코드
     */
    public RestApiException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.message = errorCode.getDefaultMessage();
    }
}
