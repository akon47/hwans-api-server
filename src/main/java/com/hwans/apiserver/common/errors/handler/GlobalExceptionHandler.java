package com.hwans.apiserver.common.errors.handler;

import com.hwans.apiserver.common.errors.errorcode.ErrorCode;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.common.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<Object> handleCustomException(RestApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        return handleExceptionInternal(errorCode, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("handleIllegalArgument", e);
        ErrorCode errorCode = ErrorCodes.BadRequest.INVALID_PARAMETER;
        return handleExceptionInternal(errorCode, e.getMessage());
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {
        log.warn("handleIllegalArgument", e);
        ErrorCode errorCode = ErrorCodes.BadRequest.INVALID_PARAMETER;
        return handleExceptionInternal(e, errorCode);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<Object> handleAllException(Exception ex) {
        log.warn("handleAllException", ex);

        ErrorCode errorCode;
        String errorMessage;
        if (ex instanceof DataIntegrityViolationException) {
            errorCode = ErrorCodes.Conflict.ALREADY_EXISTS;
            errorMessage = errorCode.getDefaultMessage();
        } else {
            errorCode = ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR;
            errorMessage = ex.getMessage();
        }

        return handleExceptionInternal(errorCode, errorMessage);
    }

    private ResponseEntity<Object> handleExceptionInternal(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(makeErrorResponse(errorCode, message));
    }

    private ErrorResponseDto makeErrorResponse(ErrorCode errorCode, String message) {
        return ErrorResponseDto.builder()
                .name(errorCode.getName())
                .message(message)
                .build();
    }

    private ResponseEntity<Object> handleExceptionInternal(BindException e, ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(makeErrorResponse(e, errorCode));
    }

    private ErrorResponseDto makeErrorResponse(BindException e, ErrorCode errorCode) {
        List<ErrorResponseDto.ValidationError> validationErrorList = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(ErrorResponseDto.ValidationError::of)
                .collect(Collectors.toList());

        return ErrorResponseDto.builder()
                .name(errorCode.getName())
                .message(e.getMessage())
                .errors(validationErrorList)
                .build();
    }
}
