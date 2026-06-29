package com.interviewlab.psychology;

import com.interviewlab.agent.MentorFeedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PsychologyService {

    public PsychologyInsight detectPattern(List<MentorFeedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return insightFor(PsychologyPattern.SOLID);
        }

        List<Integer> scores = feedbacks.stream().map(MentorFeedback::score).toList();
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(5.0);

        PsychologyPattern pattern;
        if (scores.size() >= 2 && isImproving(scores)) {
            pattern = PsychologyPattern.IMPROVING;
        } else if (avg >= 7.0) {
            pattern = PsychologyPattern.SOLID;
        } else if (avg < 4.5) {
            pattern = PsychologyPattern.NERVOUS;
        } else {
            pattern = PsychologyPattern.OVERCONFIDENT;
        }

        log.debug("PsychologyService pattern={} avgScore={} feedbacks={}", pattern, String.format("%.1f", avg), feedbacks.size());
        return insightFor(pattern);
    }

    private boolean isImproving(List<Integer> scores) {
        int midpoint = scores.size() / 2;
        double firstHalf  = scores.subList(0, midpoint).stream().mapToInt(Integer::intValue).average().orElse(0);
        double secondHalf = scores.subList(midpoint, scores.size()).stream().mapToInt(Integer::intValue).average().orElse(0);
        return secondHalf > firstHalf + 1.0;
    }

    private PsychologyInsight insightFor(PsychologyPattern pattern) {
        return switch (pattern) {
            case IMPROVING -> new PsychologyInsight(
                pattern.name(),
                "Your answers are getting stronger — you are on an upward trend. Keep it up.",
                "Focus on conciseness and structure: clear point, evidence, conclusion."
            );
            case SOLID -> new PsychologyInsight(
                pattern.name(),
                "Solid and consistent. You are showing depth and confidence.",
                "Push further with edge cases, trade-off discussions, and real production examples."
            );
            case NERVOUS -> new PsychologyInsight(
                pattern.name(),
                "Take a breath — you know more than your answers show. Slow down and think out loud.",
                "Use STAR: Situation, Task, Action, Result. Structure reduces anxiety and improves clarity."
            );
            case OVERCONFIDENT -> new PsychologyInsight(
                pattern.name(),
                "Good vocabulary — now back it up with specifics. Interviewers look for depth, not just keywords.",
                "For every concept you name, add a concrete example: a project, a decision, a trade-off you made."
            );
        };
    }
}
