package com.interviewlab.profile;

import com.interviewlab.ai.AiProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile entity — persists cross-session context for the AI agent chain.
 * user_id is both PK and FK to users.id (shared primary key pattern, 1:1).
 * No Lombok: JPA entities benefit from explicit constructor visibility control.
 * preferred_ai_provider: no hardcoded default — injected from AiProperties at creation.
 */
@Entity
@Table(
    name = "user_profiles",
    indexes = @Index(name = "idx_user_profiles_user_id", columnList = "user_id")
)
public class UserProfile {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "current_position")
    private String currentRole;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tech_stack")
    private String[] techStack;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "resume_url", columnDefinition = "TEXT")
    private String resumeUrl;

    @Column(name = "custom_prompt", columnDefinition = "TEXT")
    private String customPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_ai_provider")
    private AiProvider preferredAiProvider;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {}

    public UserProfile(UUID userId, AiProvider preferredAiProvider) {
        this.userId = userId;
        this.preferredAiProvider = preferredAiProvider;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId()                    { return userId; }
    public Integer getExperienceYears()        { return experienceYears; }
    public String getCurrentRole()             { return currentRole; }
    public String[] getTechStack()             { return techStack; }
    public String getResumeText()              { return resumeText; }
    public String getResumeUrl()               { return resumeUrl; }
    public String getCustomPrompt()            { return customPrompt; }
    public AiProvider getPreferredAiProvider() { return preferredAiProvider; }
    public Instant getUpdatedAt()              { return updatedAt; }

    public void setExperienceYears(Integer experienceYears)        { this.experienceYears = experienceYears; }
    public void setCurrentRole(String currentRole)                 { this.currentRole = currentRole; }
    public void setTechStack(String[] techStack)                   { this.techStack = techStack; }
    public void setResumeText(String resumeText)                   { this.resumeText = resumeText; }
    public void setResumeUrl(String resumeUrl)                     { this.resumeUrl = resumeUrl; }
    public void setCustomPrompt(String customPrompt)               { this.customPrompt = customPrompt; }
    public void setPreferredAiProvider(AiProvider preferredAiProvider) { this.preferredAiProvider = preferredAiProvider; }
}
