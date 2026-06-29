package com.interviewlab.assessment;

import com.interviewlab.auth.ErrorCode;
import com.interviewlab.profile.UserProfile;
import com.interviewlab.profile.UserProfileRepository;
import com.interviewlab.proficiency.Proficiency;
import com.interviewlab.proficiency.ProficiencyProperties;
import com.interviewlab.proficiency.ProficiencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class AssessmentService {

    private static final List<String> DEFAULT_TOPICS = List.of(
        "Java", "Spring Boot", "REST APIs", "Databases", "System Design",
        "Microservices", "Docker", "Kafka", "Security", "Testing"
    );

    private final UserProfileRepository userProfileRepository;
    private final ProficiencyRepository proficiencyRepository;
    private final ProficiencyProperties proficiencyProperties;

    @Transactional(readOnly = true)
    public AssessmentStartResponse startAssessment(UUID userId) {
        UserProfile profile = findProfileOrThrow(userId);
        List<String> topics = derivedTopics(profile);
        log.info("Assessment started for userId={} topics={}", userId, topics.size());
        return new AssessmentStartResponse(topics, "Rate each topic from 1 (no experience) to 10 (expert)");
    }

    @Transactional
    public void submitRatings(UUID userId, List<TopicRating> ratings) {
        for (TopicRating rating : ratings) {
            Proficiency proficiency = proficiencyRepository
                .findByUserIdAndTopic(userId, rating.topic())
                .orElseGet(() -> new Proficiency(
                    userId,
                    rating.topic(),
                    proficiencyProperties.defaultScore(),
                    proficiencyProperties.defaultSessionsCount()
                ));
            proficiency.setScore(rating.rating());
            proficiency.setSessionsCount(proficiency.getSessionsCount() + 1);
            proficiencyRepository.save(proficiency);
        }
        log.info("Assessment ratings saved: userId={} count={}", userId, ratings.size());
    }

    @Transactional(readOnly = true)
    public AssessmentReport generateReport(UUID userId) {
        List<Proficiency> proficiencies = proficiencyRepository.findByUserId(userId);
        if (proficiencies.isEmpty()) {
            throw new AssessmentException(
                ErrorCode.ASSESSMENT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "No assessment data found for user " + userId + ". Complete a self-assessment first."
            );
        }

        List<TopicScore> topics = proficiencies.stream()
            .map(p -> new TopicScore(
                p.getTopic(),
                (int) p.getScore(),
                levelFor(p.getScore()),
                recommendationFor(p.getScore())
            ))
            .sorted((a, b) -> Integer.compare(a.selfRating(), b.selfRating()))
            .toList();

        double avg = proficiencies.stream().mapToDouble(Proficiency::getScore).average().orElse(0);

        List<String> criticalGaps = proficiencies.stream()
            .filter(p -> p.getScore() < 4)
            .map(Proficiency::getTopic)
            .toList();

        List<String> quickWins = proficiencies.stream()
            .filter(p -> p.getScore() >= 4 && p.getScore() < 7)
            .map(Proficiency::getTopic)
            .toList();

        log.info("Assessment report generated: userId={} topics={} avgScore={}", userId, topics.size(), avg);
        return new AssessmentReport(topics, levelFor(avg), criticalGaps, quickWins);
    }

    private UserProfile findProfileOrThrow(UUID userId) {
        return userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new AssessmentException(
                ErrorCode.ASSESSMENT_PROFILE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "User profile not found for user " + userId + ". Complete your profile with tech stack before starting assessment."
            ));
    }

    private List<String> derivedTopics(UserProfile profile) {
        if (profile.getTechStack() == null || profile.getTechStack().length == 0) {
            return DEFAULT_TOPICS;
        }
        return Arrays.asList(profile.getTechStack());
    }

    private static String levelFor(double score) {
        if (score >= 7) return "Senior";
        if (score >= 4) return "Intermediate";
        return "Beginner";
    }

    private static String recommendationFor(double score) {
        if (score >= 8) return "Strong area — maintain depth and mentor others.";
        if (score >= 7) return "Good foundation — explore advanced patterns and edge cases.";
        if (score >= 5) return "Solid base — focus on real-world project experience.";
        if (score >= 4) return "Developing — structured practice will accelerate growth.";
        if (score >= 2) return "Early stage — start with fundamentals and build incrementally.";
        return "Uncharted — explore core concepts through hands-on exercises.";
    }
}
