package com.interviewlab.interview;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class InterviewException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public InterviewException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
