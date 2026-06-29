package com.interviewlab.assessment;

import com.interviewlab.ai.AiProvider;
import com.interviewlab.auth.ErrorCode;
import com.interviewlab.profile.UserProfile;
import com.interviewlab.profile.UserProfileRepository;
import com.interviewlab.proficiency.Proficiency;
import com.interviewlab.proficiency.ProficiencyProperties;
import com.interviewlab.proficiency.ProficiencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock UserProfileRepository userProfileRepository;
    @Mock ProficiencyRepository  proficiencyRepository;
    @Mock ProficiencyProperties  proficiencyProperties;

    @InjectMocks AssessmentService assessmentService;

    private static final UUID USER_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // Scenario 1: startAssessment — profile has tech stack → returns those topics
    // -------------------------------------------------------------------------

    @Test
    void startAssessment_withTechStack_returnsProfileTopics() {
        UserProfile profile = profileWithStack(new String[]{"Java", "Kafka", "Docker"});
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        AssessmentStartResponse response = assessmentService.startAssessment(USER_ID);

        assertThat(response.topics()).containsExactly("Java", "Kafka", "Docker");
        assertThat(response.instructions()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Scenario 2: startAssessment — empty tech stack → returns the 10 default topics
    // -------------------------------------------------------------------------

    @Test
    void startAssessment_emptyTechStack_returnsDefaultTopics() {
        UserProfile profile = profileWithStack(new String[]{});
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        AssessmentStartResponse response = assessmentService.startAssessment(USER_ID);

        assertThat(response.topics()).hasSize(10);
        assertThat(response.topics()).contains("Java", "Spring Boot", "REST APIs");
    }

    // -------------------------------------------------------------------------
    // Scenario 3: startAssessment — null tech stack → returns the 10 default topics
    // -------------------------------------------------------------------------

    @Test
    void startAssessment_nullTechStack_returnsDefaultTopics() {
        UserProfile profile = profileWithStack(null);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        AssessmentStartResponse response = assessmentService.startAssessment(USER_ID);

        assertThat(response.topics()).hasSize(10);
    }

    // -------------------------------------------------------------------------
    // Scenario 4: startAssessment — profile not found → throws ASSESSMENT_PROFILE_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void startAssessment_profileNotFound_throwsAssessmentException() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentService.startAssessment(USER_ID))
            .isInstanceOf(AssessmentException.class)
            .satisfies(ex -> {
                AssessmentException ae = (AssessmentException) ex;
                assertThat(ae.errorCode()).isEqualTo(ErrorCode.ASSESSMENT_PROFILE_NOT_FOUND);
                assertThat(ae.status()).isEqualTo(HttpStatus.NOT_FOUND);
            });
    }

    // -------------------------------------------------------------------------
    // Scenario 5: submitRatings — new topics → creates new Proficiency rows
    // -------------------------------------------------------------------------

    @Test
    void submitRatings_newTopics_savesNewProficiencyRows() {
        when(proficiencyRepository.findByUserIdAndTopic(USER_ID, "Java")).thenReturn(Optional.empty());
        when(proficiencyRepository.findByUserIdAndTopic(USER_ID, "Kafka")).thenReturn(Optional.empty());
        when(proficiencyProperties.defaultScore()).thenReturn(0.0);
        when(proficiencyProperties.defaultSessionsCount()).thenReturn(0);

        List<TopicRating> ratings = List.of(
            new TopicRating("Java", 7),
            new TopicRating("Kafka", 5)
        );

        assessmentService.submitRatings(USER_ID, ratings);

        ArgumentCaptor<Proficiency> captor = ArgumentCaptor.forClass(Proficiency.class);
        verify(proficiencyRepository, times(2)).save(captor.capture());
        List<Proficiency> saved = captor.getAllValues();
        assertThat(saved).extracting(Proficiency::getScore).containsExactlyInAnyOrder(7.0, 5.0);
    }

    // -------------------------------------------------------------------------
    // Scenario 6: submitRatings — existing topic → updates score
    // -------------------------------------------------------------------------

    @Test
    void submitRatings_existingTopic_updatesScore() {
        Proficiency existing = new Proficiency(USER_ID, "Java", 5.0, 1);
        when(proficiencyRepository.findByUserIdAndTopic(USER_ID, "Java")).thenReturn(Optional.of(existing));

        assessmentService.submitRatings(USER_ID, List.of(new TopicRating("Java", 8)));

        verify(proficiencyRepository).save(existing);
        assertThat(existing.getScore()).isEqualTo(8.0);
        assertThat(existing.getSessionsCount()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // Scenario 7: generateReport — rows exist → returns populated report
    // -------------------------------------------------------------------------

    @Test
    void generateReport_withProficiencyData_returnsReport() {
        List<Proficiency> rows = List.of(
            new Proficiency(USER_ID, "Java", 8.0, 3),
            new Proficiency(USER_ID, "Docker", 3.0, 1),
            new Proficiency(USER_ID, "Kafka", 5.0, 2)
        );
        when(proficiencyRepository.findByUserId(USER_ID)).thenReturn(rows);

        AssessmentReport report = assessmentService.generateReport(USER_ID);

        assertThat(report.topics()).hasSize(3);
        assertThat(report.criticalGaps()).containsExactly("Docker");
        assertThat(report.quickWins()).containsExactly("Kafka");
        assertThat(report.overallLevel()).isEqualTo("Intermediate"); // avg ≈ 5.3
    }

    // -------------------------------------------------------------------------
    // Scenario 8: generateReport — no proficiency data → throws ASSESSMENT_NOT_FOUND
    // -------------------------------------------------------------------------

    @Test
    void generateReport_noProficiencyData_throwsAssessmentNotFound() {
        when(proficiencyRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> assessmentService.generateReport(USER_ID))
            .isInstanceOf(AssessmentException.class)
            .satisfies(ex -> {
                AssessmentException ae = (AssessmentException) ex;
                assertThat(ae.errorCode()).isEqualTo(ErrorCode.ASSESSMENT_NOT_FOUND);
                assertThat(ae.status()).isEqualTo(HttpStatus.NOT_FOUND);
            });
    }

    private UserProfile profileWithStack(String[] techStack) {
        UserProfile profile = new UserProfile(USER_ID, AiProvider.GEMINI);
        profile.setTechStack(techStack);
        return profile;
    }
}
