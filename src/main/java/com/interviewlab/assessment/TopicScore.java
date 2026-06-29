package com.interviewlab.assessment;

public record TopicScore(
    String topic,
    int    selfRating,
    String level,
    String recommendation
) {}
