package com.interviewlab.quiz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuizQuestion(
    String       question,
    List<String> options,
    String       correctAnswer,
    String       explanation
) {}
