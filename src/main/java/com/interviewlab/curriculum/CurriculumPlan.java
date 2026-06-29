package com.interviewlab.curriculum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurriculumPlan(
    List<CurriculumItem> items,
    String               estimatedWeeks,
    String               focus
) {}
