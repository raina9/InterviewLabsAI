package com.interviewlab.auth;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = HttpStatus.UNAUTHORIZED;
    }

    public AuthException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public HttpStatus status() {
        return status;
    }
}
