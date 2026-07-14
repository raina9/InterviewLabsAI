package com.interviewlab.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewPhaseTest {

    @Test
    void questionsOneAndTwo_mapToWarmUp() {
        assertThat(InterviewPhase.forQuestionNumber(1)).isEqualTo(InterviewPhase.WARM_UP);
        assertThat(InterviewPhase.forQuestionNumber(2)).isEqualTo(InterviewPhase.WARM_UP);
    }

    @Test
    void questionsThreeToFive_mapToCoreTechnical() {
        assertThat(InterviewPhase.forQuestionNumber(3)).isEqualTo(InterviewPhase.CORE_TECHNICAL);
        assertThat(InterviewPhase.forQuestionNumber(4)).isEqualTo(InterviewPhase.CORE_TECHNICAL);
        assertThat(InterviewPhase.forQuestionNumber(5)).isEqualTo(InterviewPhase.CORE_TECHNICAL);
    }

    @Test
    void questionsSixToEight_mapToDeepDive() {
        assertThat(InterviewPhase.forQuestionNumber(6)).isEqualTo(InterviewPhase.DEEP_DIVE);
        assertThat(InterviewPhase.forQuestionNumber(7)).isEqualTo(InterviewPhase.DEEP_DIVE);
        assertThat(InterviewPhase.forQuestionNumber(8)).isEqualTo(InterviewPhase.DEEP_DIVE);
    }

    @Test
    void questionNineAndBeyond_mapToScenario() {
        assertThat(InterviewPhase.forQuestionNumber(9)).isEqualTo(InterviewPhase.SCENARIO);
        assertThat(InterviewPhase.forQuestionNumber(10)).isEqualTo(InterviewPhase.SCENARIO);
        assertThat(InterviewPhase.forQuestionNumber(50)).isEqualTo(InterviewPhase.SCENARIO);
    }

    @Test
    void zeroOrNegativeQuestionNumber_defensivelyMapsToWarmUp() {
        assertThat(InterviewPhase.forQuestionNumber(0)).isEqualTo(InterviewPhase.WARM_UP);
        assertThat(InterviewPhase.forQuestionNumber(-1)).isEqualTo(InterviewPhase.WARM_UP);
    }
}
