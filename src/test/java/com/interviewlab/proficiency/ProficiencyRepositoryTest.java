package com.interviewlab.proficiency;

import com.interviewlab.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ProficiencyRepositoryTest {

    @Autowired
    private ProficiencyRepository proficiencyRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = entityManager.persistAndFlush(
            new User("google-sub-proficiency-test", "proficiency-test@example.com", "Proficiency Test User", null)
        );
    }

    @Test
    void findByUserIdAndTopic_returnsCorrectEntry() {
        entityManager.persistAndFlush(new Proficiency(user.getId(), "java", 0.0, 0));
        entityManager.persistAndFlush(new Proficiency(user.getId(), "system-design", 0.65, 4));

        Optional<Proficiency> result = proficiencyRepository.findByUserIdAndTopic(user.getId(), "java");

        assertThat(result).isPresent();
        assertThat(result.get().getTopic()).isEqualTo("java");
        assertThat(result.get().getScore()).isEqualTo(0.0);
    }

    @Test
    void findByUserIdAndTopic_returnsEmpty_whenTopicDoesNotExist() {
        entityManager.persistAndFlush(new Proficiency(user.getId(), "java", 0.7, 3));

        Optional<Proficiency> result = proficiencyRepository.findByUserIdAndTopic(user.getId(), "kafka");

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserId_returnsAllTopicsForUser() {
        entityManager.persistAndFlush(new Proficiency(user.getId(), "java", 0.0, 0));
        entityManager.persistAndFlush(new Proficiency(user.getId(), "spring-boot", 0.5, 2));
        entityManager.persistAndFlush(new Proficiency(user.getId(), "system-design", 0.3, 1));

        List<Proficiency> results = proficiencyRepository.findByUserId(user.getId());

        assertThat(results).hasSize(3);
        assertThat(results).extracting(Proficiency::getTopic)
            .containsExactlyInAnyOrder("java", "spring-boot", "system-design");
    }

    @Test
    void uniqueConstraint_rejectsDuplicateUserTopicPair() {
        entityManager.persistAndFlush(new Proficiency(user.getId(), "java", 0.0, 0));

        Proficiency duplicate = new Proficiency(user.getId(), "java", 0.9, 10);

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
            .isInstanceOf(PersistenceException.class);
    }

    @Test
    void findByUserIdAndTopic_doesNotReturnOtherUsersEntries() {
        User otherUser = entityManager.persistAndFlush(
            new User("google-sub-other-prof", "other-prof@example.com", "Other Prof User", null)
        );
        entityManager.persistAndFlush(new Proficiency(otherUser.getId(), "java", 0.9, 10));

        Optional<Proficiency> result = proficiencyRepository.findByUserIdAndTopic(user.getId(), "java");

        assertThat(result).isEmpty();
    }
}
