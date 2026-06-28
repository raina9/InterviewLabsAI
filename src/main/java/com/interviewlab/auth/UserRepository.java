package com.interviewlab.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Primary OAuth2 lookup — google_sub is the stable identity (survives email change)
    Optional<User> findByGoogleSub(String googleSub);

    // Secondary lookup — used for deduplication and admin queries
    Optional<User> findByEmail(String email);
}
