package com.interviewlab.code;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class CodeChallengeException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;

    public CodeChallengeException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
