package com.interviewlab.config;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error response body — returned for all error HTTP responses.
 * Never exposes stack traces, Java class names, or internal state.
 * errorCode is human-readable (AUTH_TOKEN_EXPIRED, not ERROR_001).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    String errorCode,
    String message,
    int    status
) {}
