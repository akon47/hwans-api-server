package com.hwans.apiserver.common.errors.errorcode;

import org.springframework.http.HttpStatus;

public class ErrorCodes {
    public enum BadRequest implements ErrorCode {
        BAD_REQUEST,
        INVALID_REQUEST,
        INVALID_PARAMETER,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.BAD_REQUEST;
        }
    }

    public enum Unauthorized implements ErrorCode {
        UNAUTHORIZED,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.UNAUTHORIZED;
        }
    }

    public enum Forbidden implements ErrorCode {
        FORBIDDEN,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.FORBIDDEN;
        }
    }

    public enum NotFound implements ErrorCode {
        NOT_FOUND,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.NOT_FOUND;
        }
    }

    public enum Conflict implements ErrorCode {
        CONFLICT,
        ALREADY_EXISTS,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.CONFLICT;
        }
    }

    public enum InternalServerError implements ErrorCode {
        INTERNAL_SERVER_ERROR,
        ;

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public HttpStatus getStatus() {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
