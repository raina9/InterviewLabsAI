package com.interviewlab.curriculum;

import com.interviewlab.assessment.AssessmentReport;
import com.interviewlab.assessment.TopicScore;
import org.springframework.stereotype.Component;

@Component
public class CurriculumPromptBuilder {

    public String buildCurriculumPrompt(AssessmentReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a senior software engineering mentor. Generate a personalised learning curriculum ");
        sb.append("based on this candidate's self-assessment proficiency data.\n\n");
        sb.append("Overall Level: ").append(report.overallLevel()).append("\n\n");

        sb.append("Topic Proficiency (topic: self-rating/10 — level):\n");
        for (TopicScore ts : report.topics()) {
            sb.append("  - ").append(ts.topic())
              .append(": ").append(ts.selfRating()).append("/10")
              .append(" (").append(ts.level()).append(")\n");
        }

        if (!report.criticalGaps().isEmpty()) {
            sb.append("\nCritical Gaps (score < 4 — prioritise first): ")
              .append(String.join(", ", report.criticalGaps())).append("\n");
        }
        if (!report.quickWins().isEmpty()) {
            sb.append("Quick Wins (score 4–6 — high ROI): ")
              .append(String.join(", ", report.quickWins())).append("\n");
        }

        sb.append("""

                Return a JSON object with EXACTLY this structure — no preamble, no markdown, no explanation:
                {
                  "items": [
                    {
                      "topic": "topic name",
                      "priority": "HIGH",
                      "whyThisMatters": "one sentence on why this accelerates their growth",
                      "estimatedDays": "7",
                      "keyConceptsToCover": ["concept1", "concept2", "concept3"]
                    }
                  ],
                  "estimatedWeeks": "8",
                  "focus": "one sentence describing the overall curriculum theme"
                }

                Rules:
                - priority must be one of: HIGH, MEDIUM, LOW
                - Critical gaps → HIGH; quick wins → MEDIUM; strong areas → LOW
                - Order items by priority descending (HIGH first)
                - keyConceptsToCover: 3–5 items, specific and actionable
                - estimatedDays: realistic integer string (1–21)
                - Return ONLY valid JSON. No wrapping keys or arrays at the top level.
                """);

        return sb.toString();
    }
}
