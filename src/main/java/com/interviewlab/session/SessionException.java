package com.interviewlab.session;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class SessionException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public SessionException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.status    = HttpStatus.NOT_FOUND;
    }

    public SessionException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
