package com.interviewlab.ratelimit;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class RateLimitException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;

    public RateLimitException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
