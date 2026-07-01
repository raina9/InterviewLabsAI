# System Design — InterviewLab

## Problem Statement

Design an AI-powered mock interview mentorship platform. A candidate provides a resume, a target job description, target company/role (optional), experience level, and interview type (HR / Technical / System Design / Behavioural). The system runs a multi-turn mock interview, and after every candidate answer, an AI mentor evaluates the response and returns structured feedback (what was good, what to improve, a refined answer, a model answer, and a psychology note explaining why interviewers react the way they do to certain answer styles). The candidate's proficiency profile persists and evolves across sessions. The platform must also be embeddable by third parties with no build-step integration burden.

## Capacity Estimation

V1 target is a solo-founder, pre-launch, free-tier-hosted product (Railway + Vercel + Supabase). These numbers are the honest V1 assumption, not a scaled-production estimate — the point of doing this exercise explicitly is to show the reasoning, not to claim real traffic data that doesn't exist yet.

| Metric | V1 Assumption | Reasoning |
|---|---|---|
| DAU (daily active users) | 10-50 | Pre-launch / early access, personal + small public deployment modes only |
| Sessions/user/day | 1-2 | Interview practice is a deliberate, infrequent activity — not a habit-loop product |
| Turns/session | 5-10 questions | `app.agent.total-questions` config bounds this per session |
| AI calls/session | ~10-20 (1 question-gen + 1 mentor-eval per turn, roughly) | Two AI calls per turn: `InterviewAgent.nextTurn()` + `MentorAgent.analyze()` |
| Peak QPS | <1 | (50 DAU × 2 sessions × 20 calls) / 86400s ≈ 0.023 req/s sustained — bursty around active sessions, but nowhere near requiring horizontal scaling |
| Storage growth | Small | Text-heavy rows only (`messages`, `answer_feedback`) — no media storage in the core loop; `LocalFileStorageService` is used for any file uploads (resume attachments), bounded by local disk |
| AI compute cost | $0 | Ollama local inference — the entire reason [[decisions/ADR-001-ollama-over-gemini]] happened |

At this scale, the honest answer to "how do you handle the load" is: a single Railway instance with Postgres on Supabase handles it comfortably with headroom to spare. The interesting design questions are architectural (extractability, swappability, AI cost control) rather than throughput.

## High-Level Design

```
                      ┌─────────────────────┐
                      │  CDN React frontend  │  (served as static assets
                      │  (embeddable widget) │   by the same Spring process)
                      └──────────┬───────────┘
                                 │ HTTPS
                      ┌──────────▼───────────┐
                      │   SecurityConfig      │  dev token | OAuth2+JWT
                      │  (fail-closed authz)  │
                      └──────────┬───────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                         │
┌───────▼────────┐     ┌─────────▼─────────┐     ┌─────────▼─────────┐
│ SessionService  │     │ InterviewAgent /   │     │ AssessmentService /│
│ ProfileService  │     │ MentorAgent        │     │ CurriculumService /│
│                 │     │  ↓                 │     │ QuizService /      │
│                 │     │ AgentToolChain     │     │ DrillService /     │
│                 │     │  (5 tools, ordered)│     │ CodeChallengeService│
└───────┬────────┘     └─────────┬─────────┘     └─────────┬─────────┘
        │                        │                         │
        │              ┌─────────▼─────────┐               │
        │              │ AIProviderFactory  │               │
        │              │  (Strategy)        │               │
        │              │  Ollama (active)   │               │
        │              │  Gemini/Claude/    │               │
        │              │  OpenAI (parked)   │               │
        │              └────────────────────┘               │
        │                                                    │
        └────────────────────────┬───────────────────────────┘
                                  │
                       ┌──────────▼───────────┐
                       │  PostgreSQL (Supabase) │
                       │  Flyway-versioned      │
                       └───────────────────────┘

        EventPublisher (SyncEventPublisher active; KafkaEventPublisher parked)
        StorageService (LocalFileStorageService active; S3StorageService parked)
        AgentOrchestrator (LocalAgentOrchestrator active; CloudAgentOrchestrator parked)
```

### Components

See `hld.md` for the full component responsibility table; the short version: `SecurityConfig` gates every request, domain services (`SessionService`, `InterviewService`, etc.) own their own controller/service/repository slice, and every AI-driven feature routes through `AIProviderFactory` so the provider is never hardcoded into a domain service.

### Data Flow — Interview Turn (also documented in `hld.md`)

`POST /api/v1/interview/{sessionId}/respond` → auth → ownership check → `InterviewAgent.nextTurn()` (tool chain → prompt build → AI call → persist next question) → `MentorAgent.analyze()` (tool chain → prompt build → AI call → parse JSON → persist feedback) → `EventPublisher.publishAnswerScored()` → `PsychologyService.detectPattern()` every 3rd answer → response.

Two AI calls per turn is the core cost/latency driver — worth calling out explicitly in any interview discussion of this design, since it's the first thing that would need caching or batching consideration at higher scale.

## Deep Dives

### AI Abstraction
`AiProviderStrategy` (Strategy pattern) + `AIProviderFactory` (Factory pattern) decouple every caller from the concrete provider. This is the single most load-bearing design decision in the system — it's what made [[decisions/ADR-001-ollama-over-gemini]] a config change instead of a rewrite, and it's what makes future per-user provider preference (`user_profiles.preferred_ai_provider`, currently written but not read back — see [[ai-architecture]]) a small addition rather than a redesign.

### Agent Context Management
`AgentToolChain` assembles context from five independently-failing sources (resume, sliding-window history, proficiency, Q&A reference [stub], custom prompt) before every AI call. The chain-never-aborts design (a failing tool contributes an empty string, logged as a warning) is a deliberate reliability choice: a broken data source degrades answer quality, but never breaks the interview flow entirely. This trades silent degradation for availability — worth naming explicitly as a trade-off, since silent degradation has its own risk (a candidate never knows their resume context didn't load).

### Concurrent Sessions
Nothing in the current design explicitly locks or serializes access to a single session — two concurrent requests against the same `sessionId` (e.g. a duplicate button click submitting the same answer twice) are not guarded by an idempotency key or optimistic locking at the API layer. This is an honest current gap, not a documented "handled" behavior — worth flagging directly if asked "how do you prevent duplicate answer submission," since the honest answer is "not yet, and here's how I'd add it" (idempotency key on the `respond` endpoint, or an optimistic lock/version column on `sessions`).

## Trade-offs

| Decision | Trade-off | See |
|---|---|---|
| Monolith over microservices | Simpler ops, harder independent horizontal scaling later | [[decisions/ADR-002-monolith-first]] |
| CDN React over bundled SPA | Zero-build embeddability, manual dependency wiring, no tree-shaking | [[decisions/ADR-003-cdn-react-over-bundled]] |
| Local LLM (Ollama) over hosted API | Zero cost, weaker model quality, cold-start latency | [[decisions/ADR-001-ollama-over-gemini]] |
| Sync event publishing over Kafka | Zero infra, but no async decoupling, no multi-instance-safe event delivery yet | [[decisions/ADR-004-conditional-on-property-pattern]] |

## At 10x Scale — What Changes, What Stays

**What changes:**
- `MESSAGING_MODE=kafka` gets flipped — `KafkaEventPublisher` (not yet built, seam ready) replaces `SyncEventPublisher` so answer-scoring and session-completion side effects stop blocking the request thread
- `AI_PROVIDER` likely moves off Ollama for at least some traffic tier — local inference doesn't horizontally scale the way a hosted API does, and the cost-rule calculus changes once there's revenue to justify it
- The IDOR gap in `/api/v1/assessment/report/{userId}` and `/api/v1/curriculum/{userId}` (see [[authz-matrix]]) stops being a "low-traffic, low-risk" gap and becomes a must-fix — attack surface scales with user count
- Redis-backed session state (currently parked, see CLAUDE.md `PARKED ITEMS`) becomes necessary the moment there's more than one backend instance — the in-memory quiz/drill/code stores are explicitly single-instance-only today
- Rate limiting on AI call volume per user becomes necessary — nothing currently bounds how many AI calls one account can trigger

**What stays:**
- Domain package boundaries — the monolith's internal structure doesn't need to change to support extraction; it was drawn for this from the start ([[decisions/ADR-002-monolith-first]])
- The Strategy/Factory AI provider abstraction — switching providers or adding a new one is unaffected by scale
- The Swappable Backend Pattern itself ([[decisions/ADR-004-conditional-on-property-pattern]]) — it's the mechanism that makes every "what changes at 10x" item above a config flip instead of a rewrite

See also: [[hld]], [[ai-architecture]], [[authz-matrix]]
