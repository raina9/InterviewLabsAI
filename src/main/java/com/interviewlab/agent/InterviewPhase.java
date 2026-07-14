package com.interviewlab.agent;

/**
 * Interview structure — Plan A: prompt-level phases derived from question count.
 * No schema change, no new entity — the phase is computed on the fly from the
 * question number already tracked via session message count (see InterviewAgent).
 */
public enum InterviewPhase {

    WARM_UP(
        "WARM-UP",
        "Ask about the candidate's experience overview, current role, or the project "
            + "they know best. Keep it easy and confidence-building."
    ),
    CORE_TECHNICAL(
        "CORE TECHNICAL",
        "Ask about fundamentals from the job description's primary skills. Medium difficulty."
    ),
    DEEP_DIVE(
        "DEEP-DIVE",
        "Follow up on the candidate's weakest answers so far — use the proficiency and "
            + "session context provided below. Probe for depth, not breadth."
    ),
    SCENARIO(
        "SCENARIO",
        "Ask a system design or real-world production question matching the job "
            + "description. Hard difficulty."
    );

    private final String label;
    private final String instruction;

    InterviewPhase(String label, String instruction) {
        this.label = label;
        this.instruction = instruction;
    }

    public String label() {
        return label;
    }

    public String instruction() {
        return instruction;
    }

    /**
     * questionNumber is 1-indexed (the question about to be asked).
     * 1-2: WARM_UP | 3-5: CORE_TECHNICAL | 6-8: DEEP_DIVE | 9+: SCENARIO.
     * Anything <= 2, including 0/negative, defensively resolves to WARM_UP.
     */
    public static InterviewPhase forQuestionNumber(int questionNumber) {
        if (questionNumber <= 2) {
            return WARM_UP;
        }
        if (questionNumber <= 5) {
            return CORE_TECHNICAL;
        }
        if (questionNumber <= 8) {
            return DEEP_DIVE;
        }
        return SCENARIO;
    }
}
