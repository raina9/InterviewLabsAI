# InterviewLab — Database Schema

All tables managed by Flyway migrations under `src/main/resources/db/migration/`.  
Primary keys are UUID v4 generated client-side.  
All timestamps are `TIMESTAMP WITH TIME ZONE` stored in UTC.

---

## users

```sql
CREATE TABLE users (
    id          UUID PRIMARY KEY,
    google_sub  VARCHAR(255) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    picture     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Internal application identity |
| google_sub | VARCHAR(255) | Google OAuth2 `sub` claim — unique per Google account |
| email | VARCHAR(255) | From Google token — not editable by the user |
| name | VARCHAR(255) | Display name from Google |
| picture | TEXT | Avatar URL from Google — nullable |
| created_at | TIMESTAMPTZ | Account creation time |

---

## user_profiles

```sql
CREATE TABLE user_profiles (
    user_id              UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    experience_years     INTEGER,
    current_position     VARCHAR(255),
    tech_stack           TEXT[],
    resume_text          TEXT,
    custom_prompt        TEXT,
    preferred_ai_provider VARCHAR(50),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

| Column | Type | Notes |
|---|---|---|
| user_id | UUID | 1:1 with users — not a separate auto-increment PK |
| experience_years | INTEGER | Self-reported; used by AssessmentService |
| current_position | VARCHAR(255) | e.g. "Senior Software Engineer" |
| tech_stack | TEXT[] | PostgreSQL array; e.g. `{Java, Spring Boot, Kafka}` |
| resume_text | TEXT | ATS paste; injected into interview context via ResumeContextTool |
| custom_prompt | TEXT | Saved candidate instruction appended to every session prompt |
| preferred_ai_provider | VARCHAR(50) | OLLAMA / GEMINI / CLAUDE / OPENAI — overrides app default |
| updated_at | TIMESTAMPTZ | Updated on every PUT /profile call |

---

## sessions

```sql
CREATE TABLE sessions (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interview_type  VARCHAR(50) NOT NULL,
    target_company  VARCHAR(255),
    target_role     VARCHAR(255),
    jd_text         TEXT,
    topic_focus     VARCHAR(255),
    difficulty      VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Session identifier used in all downstream relations |
| user_id | UUID | FK to users |
| interview_type | VARCHAR(50) | TECHNICAL / HR / SYSTEM_DESIGN / BEHAVIOURAL |
| target_company | VARCHAR(255) | Optional; injected into prompt context |
| target_role | VARCHAR(255) | e.g. "Staff Backend Engineer" |
| jd_text | TEXT | Full JD text pasted by candidate |
| topic_focus | VARCHAR(255) | e.g. "Kafka, Distributed Systems" |
| difficulty | VARCHAR(20) | EASY / MEDIUM / HARD |
| status | VARCHAR(20) | ACTIVE / COMPLETED / ABANDONED |
| created_at | TIMESTAMPTZ | Session start time |
| completed_at | TIMESTAMPTZ | Null while active; set on COMPLETED or ABANDONED transition |

---

## messages

```sql
CREATE TABLE messages (
    id          UUID PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    sequence    INTEGER NOT NULL,
    voice_used  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (session_id, sequence)
);

CREATE INDEX idx_messages_session_id ON messages(session_id);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Message identity |
| session_id | UUID | FK to sessions |
| role | VARCHAR(20) | INTERVIEWER or CANDIDATE |
| content | TEXT | The message text |
| sequence | INTEGER | Turn number within the session — enforces ordering, prevents duplicates |
| voice_used | BOOLEAN | True if candidate submitted via voice transcription |
| created_at | TIMESTAMPTZ | Insertion time |

---

## answer_feedback

```sql
CREATE TABLE answer_feedback (
    id                UUID PRIMARY KEY,
    session_id        UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    message_id        UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    question          TEXT NOT NULL,
    candidate_answer  TEXT NOT NULL,
    refined_answer    TEXT,
    model_answer      TEXT,
    score             INTEGER NOT NULL CHECK (score BETWEEN 1 AND 10),
    feedback_good     TEXT,
    feedback_improve  TEXT,
    psychology_note   TEXT,
    scored_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_answer_feedback_session_id ON answer_feedback(session_id);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Feedback identity |
| session_id | UUID | FK to sessions — used for session-level report queries |
| message_id | UUID | FK to the CANDIDATE message being evaluated |
| question | TEXT | The question that was asked (for display in report) |
| candidate_answer | TEXT | The raw candidate answer |
| refined_answer | TEXT | MentorAgent-improved version of the candidate's answer |
| model_answer | TEXT | What an expert answer looks like |
| score | INTEGER | 1–10; CHECK constraint enforced at DB level |
| feedback_good | TEXT | What worked in the answer |
| feedback_improve | TEXT | What to improve |
| psychology_note | TEXT | Per-answer self-management note from MentorAgent |
| scored_at | TIMESTAMPTZ | When MentorAgent completed evaluation |

---

## proficiency

```sql
CREATE TABLE proficiency (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic           VARCHAR(255) NOT NULL,
    score           NUMERIC(4,2) NOT NULL,
    sessions_count  INTEGER NOT NULL DEFAULT 0,
    last_updated    TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (user_id, topic)
);

CREATE INDEX idx_proficiency_user_id ON proficiency(user_id);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Row identity |
| user_id | UUID | FK to users |
| topic | VARCHAR(255) | e.g. "Java", "Kafka", "System Design" |
| score | NUMERIC(4,2) | Running average score (1.00–10.00) across all answers on this topic |
| sessions_count | INTEGER | How many sessions contributed to this score |
| last_updated | TIMESTAMPTZ | Updated after each answer.scored event |

UNIQUE (user_id, topic) ensures one row per topic per user — AssessmentService uses upsert.

---

## system_feedback

```sql
CREATE TABLE system_feedback (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id    UUID REFERENCES sessions(id) ON DELETE SET NULL,
    feedback_text TEXT NOT NULL,
    applied       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

| Column | Type | Notes |
|---|---|---|
| id | UUID | Feedback identity |
| user_id | UUID | Who submitted the feedback |
| session_id | UUID | Which session prompted it — nullable (general feedback) |
| feedback_text | TEXT | Free-form user feedback about the platform |
| applied | BOOLEAN | Admin flag — toggled when feedback is actioned |
| created_at | TIMESTAMPTZ | Submission time |

---

## Why This Shape

**One proficiency row per user per topic, not one per session**  
A new row per session would require averaging across all rows to compute a score. A single upserted row with a running average is O(1) to read and write. The tradeoff: individual session-level score history is lost. For V1, this is acceptable — cross-session trend analysis is a P2 feature.

**user_profiles is 1:1 with users, not embedded**  
The profile contains heavy TEXT fields (resume, custom prompt). Embedding them in the users table would bloat every auth query that only needs id/email/name. Separation lets the auth path read the small users row without pulling resume text.

**answer_feedback stores question + candidate_answer redundantly**  
Copying question and candidate_answer into answer_feedback makes the feedback table self-contained for report generation. The alternative — joining messages at report time — adds complexity and makes the messages table load-bearing for the feedback loop. At 5–15 questions per session, the storage cost is negligible.

**messages.sequence has a UNIQUE constraint, not just an index**  
Sequence integrity is a data invariant, not a performance concern. Without the unique constraint, a race condition (two concurrent answers to the same session) could insert duplicate sequence numbers, corrupting the conversation order that SessionHistoryTool relies on.

**system_feedback.session_id uses ON DELETE SET NULL, not CASCADE**  
Deleting a session should not delete platform feedback — that data is independent of the session lifecycle. SET NULL preserves the feedback row and marks that the session it referenced no longer exists.

**No separate users_to_roles or permissions table**  
V1 has one user type. Spring Security roles are derived from the JWT claim, not from a DB table. When multi-role is needed, the JWT claim approach allows adding roles without a schema migration.

**In-memory stores for Quiz/Drill/CodeChallenge are intentional, not a schema gap**  
Quiz, Drill, and Code Challenge sessions are transient — they live for a single interaction and are discarded on result/summary retrieval. Persisting them would require schema migrations, GC queries, and TTL management for no user-facing value in V1. The tradeoff is documented: not horizontally scalable (see PARKED ITEMS — Redis-backed session store).
