package com.interviewlab.interview;

import com.interviewlab.agent.MentorFeedback;
import com.interviewlab.feedback.AnswerFeedback;

public record MentorFeedbackResponse(
    String feedbackGood,
    String feedbackImprove,
    String refinedAnswer,
    String modelAnswer,
    String psychologyNote,
    int    score
) {
    public static MentorFeedbackResponse from(MentorFeedback feedback) {
        return new MentorFeedbackResponse(
            feedback.feedbackGood(),
            feedback.feedbackImprove(),
            feedback.refinedAnswer(),
            feedback.modelAnswer(),
            feedback.psychologyNote(),
            feedback.score()
        );
    }

    public static MentorFeedbackResponse from(AnswerFeedback feedback) {
        return new MentorFeedbackResponse(
            feedback.getFeedbackGood(),
            feedback.getFeedbackImprove(),
            feedback.getRefinedAnswer(),
            feedback.getModelAnswer(),
            feedback.getPsychologyNote(),
            feedback.getScore()
        );
    }
}
