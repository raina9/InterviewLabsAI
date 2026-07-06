package com.interviewlab.ai;

import com.interviewlab.auth.ErrorCode;
import org.springframework.http.HttpStatus;

public class AIProviderException extends RuntimeException {

    private final ErrorCode  errorCode;
    private final HttpStatus status;
    private final Long       retryAfterSeconds;

    public AIProviderException(ErrorCode errorCode, HttpStatus status, String message) {
        this(errorCode, status, message, null);
    }

    // retryAfterSeconds: carries the Retry-After header value (AI_BUSY only) so
    // GlobalExceptionHandler needs no AiQueueProperties dependency of its own — that would
    // force every @WebMvcTest slice in the suite to provide a @ConfigurationProperties bean
    // it has no other reason to load (see docs/decisions/ADR-011).
    public AIProviderException(ErrorCode errorCode, HttpStatus status, String message, Long retryAfterSeconds) {
        super(message);
        this.errorCode         = errorCode;
        this.status            = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode  errorCode()         { return errorCode; }
    public HttpStatus status()            { return status; }
    public Long       retryAfterSeconds() { return retryAfterSeconds; }
}
