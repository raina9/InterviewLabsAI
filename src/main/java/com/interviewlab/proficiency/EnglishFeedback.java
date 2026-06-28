package com.interviewlab.proficiency;

public record EnglishFeedback(
    String fluencyNote,
    String tenseFeedback,
    String fillerWordsDetected,
    String vocabularyNote,
    String confidenceNote,
    String improvedVersion,
    int    fluencyScore
) {}
