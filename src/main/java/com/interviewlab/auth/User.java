package com.interviewlab.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User identity entity — Google OAuth2 account linked by OIDC sub claim.
 * No Lombok: JPA entities benefit from explicit no-arg constructor visibility control.
 * Records cannot be JPA entities (mutable state required by Hibernate proxy).
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_google_sub", columnList = "google_sub", unique = true),
        @Index(name = "idx_users_email",      columnList = "email",      unique = true)
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Google OIDC sub — stable unique identifier that survives email changes
    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    private String googleSub;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Google profile picture URL — mutable (user can change Google avatar)
    @Column(name = "picture", length = 500)
    private String picture;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Java-side default mirrors the DB DEFAULT 'CANDIDATE' (V9__add_user_role.sql) —
    // this ensures Hibernate always sends an explicit value on INSERT rather than
    // relying on the column being omitted (it isn't; Hibernate inserts all mapped columns).
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.CANDIDATE;

    // Required by JPA / Hibernate proxy — not for application use
    protected User() {}

    public User(String googleSub, String email, String name, String picture) {
        this.googleSub  = googleSub;
        this.email      = email;
        this.name       = name;
        this.picture    = picture;
        this.createdAt  = Instant.now();
    }

    public UUID getId()        { return id; }
    public String getGoogleSub() { return googleSub; }
    public String getEmail()   { return email; }
    public String getName()    { return name; }
    public String getPicture() { return picture; }
    public Instant getCreatedAt() { return createdAt; }
    public Role   getRole()    { return role; }

    // Name and picture are mutable — updated on each login from fresh OAuth2 attributes
    public void setName(String name)       { this.name = name; }
    public void setPicture(String picture) { this.picture = picture; }
}
