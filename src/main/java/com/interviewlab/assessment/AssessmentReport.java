package com.interviewlab.assessment;

import java.util.List;

public record AssessmentReport(
    List<TopicScore> topics,
    String           overallLevel,
    List<String>     criticalGaps,
    List<String>     quickWins
) {}
