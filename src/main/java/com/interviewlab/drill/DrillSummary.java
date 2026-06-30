package com.interviewlab.drill;

import java.util.List;

public record DrillSummary(
    String       topic,
    DrillMode    mode,
    int          questionsAnswered,
    double       avgScore,
    List<String> weakSpots,
    List<String> strongPoints
) {}
