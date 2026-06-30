package com.interviewlab.code;

public record CodeSubmitResponse(
    boolean passed,
    String  feedback,
    String  refinedCode,
    String  explanation,
    String  executionResult
) {}
