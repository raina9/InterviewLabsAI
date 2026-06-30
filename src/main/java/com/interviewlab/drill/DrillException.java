package com.interviewlab.drill;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class DrillException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;

    public DrillException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
