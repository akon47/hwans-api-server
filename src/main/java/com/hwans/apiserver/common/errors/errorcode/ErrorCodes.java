package com.hwans.apiserver.common.errors.errorcode;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

public class ErrorCodes {
    @RequiredArgsConstructor
    public enum BadRequest implements ErrorCode {
        BAD_REQUEST("잘못된 요청입니다."),
        INVALID_PARAMETER("잘못된 파라미터입니다."),
        INVALID_EMAIL_VERIFY_CODE("잘못된 인증 코드입니다."),
        ;

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.BAD_REQUEST;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }

    @RequiredArgsConstructor
    public enum Unauthorized implements ErrorCode {
        UNAUTHORIZED("유효한 자격증명이 존재하지 않습니다."),
        INVALID_REFRESH_TOKEN("유효한 RefreshToken이 아닙니다."),
        TOKEN_EXPIRED("토큰이 만료되었습니다.");

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.UNAUTHORIZED;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }

    @RequiredArgsConstructor
    public enum Forbidden implements ErrorCode {
        FORBIDDEN("접근 권한이 없습니다."),
        ;

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.FORBIDDEN;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }

    @RequiredArgsConstructor
    public enum NotFound implements ErrorCode {
        NOT_FOUND("존재하지 않습니다."),
        NO_CURRENT_ACCOUNT_INFO("현재 계정에 대한 정보를 찾을 수 없습니다."),
        NOT_FOUND_POST("게시글을 찾을 수 없습니다."),
        ;

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.NOT_FOUND;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }

    @RequiredArgsConstructor
    public enum Conflict implements ErrorCode {
        CONFLICT("요청이 충돌하여 요청을 완료 할 수 없습니다."),
        ALREADY_EXISTS("이미 존재합니다."),
        ALREADY_EXISTS_EMAIL("이미 존재하는 사용자 계정 이메일 입니다."),
        ALREADY_EXISTS_BLOG_ID("이미 존재하는 블로그 Id 입니다."),
        ALREADY_EXISTS_POST_URL("이미 존재하는 게시글 URL 입니다."),
        ALREADY_EXISTS_VERIFY_CODE("이전에 발송한 인증 코드가 아직 유효합니다.")
        ;

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.CONFLICT;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }

    @RequiredArgsConstructor
    public enum InternalServerError implements ErrorCode {
        INTERNAL_SERVER_ERROR("서버 내부 오류입니다."),
        ;

        private final String defaultMessage;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        @Override
        public String getDefaultMessage() {
            return this.defaultMessage;
        }
    }
}
