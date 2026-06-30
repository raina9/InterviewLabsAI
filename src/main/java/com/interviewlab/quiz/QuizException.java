package com.interviewlab.quiz;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class QuizException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;

    public QuizException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
