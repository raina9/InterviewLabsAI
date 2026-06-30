package com.interviewlab.code;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Judge0SubmissionResult(
    Judge0Status status,
    String       stdout,
    String       stderr,
    String       time,
    Long         memory
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Judge0Status(int id, String description) {}

    // Status IDs: 3=Accepted, 4=Wrong Answer, 5=TLE, 6=Compilation Error, 11=Runtime Error (NZEC)
    public boolean accepted() { return status != null && status.id() == 3; }
}
