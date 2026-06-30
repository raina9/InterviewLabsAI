# InterviewLab — High-Level Design

## Plain English

InterviewLab is an AI-powered interview mentorship platform. A candidate pastes their resume and a job description, selects an interview type (HR, Technical, System Design, Behavioural), and the system runs a context-aware mock interview. After each answer, a Mentor agent evaluates the response and immediately returns structured feedback: what was good, what to improve, a refined version of the answer, and a model answer. The candidate's performance accumulates into a proficiency profile that persists across sessions.

The system runs as a single Spring Boot monolith backed by PostgreSQL. All AI calls go through a Strategy-pattern provider interface — Ollama (local, zero cost) is active by default; Gemini can be switched in via an environment variable with no code change. The frontend is a CDN-React single-page app served by the same process.

### Core User Journey

```
Candidate fills IntakeForm (resume + JD + role + type)
  → SessionService creates a Session row (status=ACTIVE)
    → InterviewAgent.initSession() builds context, generates first question
      → Candidate answers
        → InterviewAgent.nextTurn() generates follow-up question
        → MentorAgent.analyze() scores the answer, produces structured feedback
          → AnswerFeedback persisted, proficiency updated
          → PsychologyService checks pattern every 3rd answer (nudge surfaced in UI)
```

### Component Breakdown

| Component | Responsibility |
|---|---|
| SecurityConfig | Auth mode switching: dev (X-Dev-Token) or oauth (Google + JWT cookie) |
| InterviewAgent | Question generation, session flow management |
| MentorAgent | Answer scoring, feedback, refined/model answers, psychology note |
| AgentToolChain | Injects context (resume, history, proficiency, Q&A refs, custom prompt) into prompts |
| AIProviderFactory | Selects active AI provider via env var (OLLAMA / GEMINI / CLAUDE / OPENAI) |
| AssessmentService | Self-rating intake → proficiency row upserts |
| CurriculumService | Reads AssessmentReport → generates personalised learning plan via AI |
| PsychologyService | Detects performance patterns from score history (NERVOUS / IMPROVING / SOLID / OVERCONFIDENT) |
| QuizService | Generates MCQ quiz, scores answers, returns result |
| DrillService | RAPID (10 Q&A) or DEEP (Socratic) topic drilling |
| CodeChallengeService | Generates coding challenge, evaluates via Judge0 (or AI fallback) |
| EnglishService | Analyses transcript for fluency, grammar, vocabulary, filler words |
| EventPublisher | Observer V1 (sync, in-process); V2 = KafkaEventPublisher (parked) |

### Data Flow: Interview Turn

```
POST /api/v1/interview/{sessionId}/respond
  → SecurityConfig: JWT or DevToken → AuthenticatedUser
  → InterviewService.respond()
    → AssertOwnership, AssertActive
    → InterviewAgent.nextTurn()
        → AgentToolChain.execute() → Map<toolName, contextString>
        → InterviewAgentPromptBuilder.buildFollowUpPrompt()
        → AIProvider.generate() → nextQuestion
        → MessageService.addMessage(INTERVIEWER, nextQuestion)
    → MentorAgent.analyze()
        → AgentToolChain.execute() → context
        → MentorAgentPromptBuilder.buildFeedbackPrompt()
        → AIProvider.generateJson() → JSON feedback
        → parseFeedback() → MentorFeedback record
    → AnswerFeedbackRepository.save()
    → EventPublisher.publishAnswerScored()
    → PsychologyService.detectPattern() (every 3rd answer)
  → InterviewTurnResponse(nextQuestion, sessionComplete, mentorFeedback, psychologyNudge)
```

### Technology Decisions

| Decision | Choice | Why |
|---|---|---|
| AI strategy pattern | Sealed interface + factory | Switch provider via env var, no code change |
| Stateless auth | JWT cookie | No session state server-side, scales horizontally |
| In-process events (V1) | SyncEventPublisher | Zero infra dependency for single-instance V1 |
| CDN React | Babel standalone | No build step, no Node.js server, served from Spring |
| Virtual threads | spring.threads.virtual.enabled=true | All I/O-bound (AI calls, DB) benefit automatically |
| Single application.yml | No Spring Profiles | Railway deploys a single binary; env vars control all modes |

---

## Interview Talking Points

**"Walk me through the InterviewLab architecture."**
> Single Spring Boot monolith. Feature-per-package layout. PostgreSQL for persistence, Flyway for migrations. AI abstracted behind a Strategy interface — Ollama or Gemini via env var. Frontend is a CDN React SPA served by the same process. Virtual threads enabled for IO-bound operations.

**"How does the AI layer work?"**
> AiProviderStrategy interface with generate() and generateJson(). AIProviderFactory selects the implementation at startup from an env var. Each agent constructs its own prompt via a PromptBuilder that injects toolchain context, then calls the provider with per-call AIOptions (temperature, maxTokens). The provider handles retries for transient errors and maps HTTP 429/5xx to domain exceptions.

**"Explain the AgentTool Chain of Responsibility."**
> AgentToolChain holds a List<AgentTool> in @Order sequence. Each tool receives AgentContext and returns a named string of context (resume text, session history, proficiency summary, Q&A references, custom prompt). The chain never aborts on a single tool failure — it logs a warning and contributes an empty string. All tool outputs are merged into a Map<String,String> that the PromptBuilder injects as sections.

**"How is auth mode switching implemented?"**
> SecurityConfig inspects AUTH_MODE env var at bean construction time. If AUTH_MODE=oauth or GOOGLE_CLIENT_ID + GOOGLE_CLIENT_SECRET are both set (auto-detect), it configures oauth2Login + JwtAuthFilter. Otherwise it falls back to DevTokenFilter. No Spring Profile, no code change — a single binary deployed with different env vars.

**"Why is the EventPublisher abstracted?"**
> To evolve from synchronous in-process events (V1, zero infra) to Kafka (V2, async at scale) without changing any caller. SyncEventPublisher is the only concrete @Component today. When KafkaEventPublisher is added, it will be activated via @ConditionalOnProperty — same interface, bean swap, callers untouched.

**"What are the known scaling constraints?"**
> Quiz, Drill, and Code Challenge sessions are held in ConcurrentHashMap on the local JVM. A multi-instance deploy would route subsequent requests to different pods, causing NOT_FOUND errors. Fix path: Redis-backed session store. Blocked on: public deployment trigger. Documented in PARKED ITEMS.
