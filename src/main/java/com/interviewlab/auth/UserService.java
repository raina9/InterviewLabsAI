package com.interviewlab.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    /**
     * Idempotent upsert: finds user by Google sub, creates if absent, updates if profile changed.
     * V1 race-condition note: fine for personal/single-instance deployment. V2 (multi-instance):
     * replace with a single UPSERT SQL via @Query to eliminate the read-then-write window.
     */
    @Transactional
    public User findOrCreate(String googleSub, String email, String name, String picture) {
        return userRepository.findByGoogleSub(googleSub)
                .map(existing -> syncProfile(existing, name, picture))
                .orElseGet(() -> {
                    User created = userRepository.save(new User(googleSub, email, name, picture));
                    log.info("New user registered: id={} email={}", created.getId(), created.getEmail());
                    return created;
                });
    }

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AuthException(
                    ErrorCode.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "User with id " + id + " not found"
                ));
    }

    // Name and picture may change when a user updates their Google account
    private User syncProfile(User user, String name, String picture) {
        boolean dirty = false;
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            dirty = true;
        }
        if (picture != null && !picture.equals(user.getPicture())) {
            user.setPicture(picture);
            dirty = true;
        }
        if (dirty) {
            log.debug("Synced Google profile for user id={}", user.getId());
        }
        return user;
    }
}
