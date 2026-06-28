package com.interviewlab.proficiency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProficiencyRepository extends JpaRepository<Proficiency, UUID> {

    Optional<Proficiency> findByUserIdAndTopic(UUID userId, String topic);

    List<Proficiency> findByUserId(UUID userId);
}
