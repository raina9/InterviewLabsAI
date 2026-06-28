package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class AIProviderException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;

    public AIProviderException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
