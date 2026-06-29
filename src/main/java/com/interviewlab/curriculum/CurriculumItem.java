package com.interviewlab.curriculum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurriculumItem(
    String       topic,
    String       priority,
    String       whyThisMatters,
    String       estimatedDays,
    List<String> keyConceptsToCover
) {}
