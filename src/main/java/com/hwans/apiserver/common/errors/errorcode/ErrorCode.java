package com.hwans.apiserver.common.errors.errorcode;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    /**
     * 에러의 이름을 반환한다.
     * 
     * @return 에러 이름
     */
    String getName();

    /**
     * HTTP Status를 반환한다.
     * 
     * @return HTTP Status
     */
    HttpStatus getStatus();

    /**
     * 에러 메시지를 반환한다.
     * 
     * @return 에러 메시지
     */
    String getDefaultMessage();
}
