package com.interviewlab.profile;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class ProfileException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public ProfileException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
        this.status    = HttpStatus.NOT_FOUND;
    }

    public ProfileException(ErrorCode errorCode, HttpStatus status, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status    = status;
    }

    public ErrorCode  errorCode() { return errorCode; }
    public HttpStatus status()    { return status; }
}
