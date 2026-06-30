package com.interviewlab.code;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeChallenge(
    UUID                id,
    String              title,
    String              description,
    Map<String, String> starterCode,   // language → starter code skeleton
    List<String>        testCases,
    List<String>        constraints
) {}
