# InterviewLab — Revision Notes

One-page fast-recall summary. Checkpoints 1–10 complete. Current: V1 feature-complete.

---

## Architecture in one sentence

Single Spring Boot 4.1.0 monolith. Feature-per-package. PostgreSQL + Flyway. AI behind a Strategy interface. CDN React SPA served from the same process. Virtual threads on.

---

## Auth (Checkpoint 2)

Two modes, same binary:
- `AUTH_MODE=dev` → DevTokenFilter → `X-Dev-Token: dev-secret` header
- `AUTH_MODE=oauth` (or GOOGLE_CLIENT_ID + GOOGLE_CLIENT_SECRET both set) → Google OAuth2 + JwtAuthFilter + JWT cookie

Principal: `AuthenticatedUser(id, email, name, picture)` — injected by `@AuthenticationPrincipal`.  
SecurityConfig reads env vars at bean construction — no Spring Profiles, no code change needed to switch modes.

---

## Schema (Checkpoint 3)

7 tables: `users` → `user_profiles` (1:1) → `sessions` → `messages` → `answer_feedback`  
Separate: `proficiency` (one row per user+topic, upsert), `system_feedback`

Key constraints: `messages(session_id, sequence)` is UNIQUE. `answer_feedback.score` is CHECK(1–10). `proficiency(user_id, topic)` is UNIQUE.

In-memory only (no DB table): Quiz sessions, Drill sessions, Code Challenge sessions.

---

## AI Layer (Checkpoint 5)

**Strategy**: `AIProvider` sealed interface → `generate()` + `generateJson()`  
**Factory**: `AIProviderFactory.getDefaultProvider()` reads `DEFAULT_AI_PROVIDER` env var  
**Active**: Ollama (local, zero cost) or Gemini (API key required)  
**Parked**: Claude, OpenAI — beans instantiated, not active  
**AIOptions**: `defaults()` / `forFeedback()` / `forQuestions()` — static factory, hardcoded values  
**Dead config**: `AiProperties.OptionsConfig` — bound from yml, never consumed at runtime

---

## Agent Architecture (Checkpoint 6)

**InterviewAgent**: question generation + session flow only  
**MentorAgent**: scoring + feedback + refined answer + model answer + psychology note  

**AgentToolChain** (Chain of Responsibility, @Order):
1. ResumeContextTool
2. SessionHistoryTool
3. ProficiencyTool
4. QnAReferenceTool
5. UserPromptTool

Chain never aborts on tool failure — logs warn, contributes empty string.

---

## Interview Flow (Checkpoint 7)

```
POST /interview/start → InterviewAgent.initSession() → first question
POST /interview/{id}/respond → InterviewAgent.nextTurn() + MentorAgent.analyze()
  → AnswerFeedback saved → EventPublisher.publishAnswerScored()
  → PsychologyService.detectPattern() every 3rd scored answer
  → InterviewTurnResponse(nextQuestion, sessionComplete, mentorFeedback, psychologyNudge)
```

Session status: ACTIVE → COMPLETED or ABANDONED (explicit POST)

---

## Voice (Checkpoint 8)

Client-side STT: browser `SpeechRecognition` API → transcript sent to `/interview/respond`  
`messages.voice_used = true` when voice was used  
Server-side STT endpoint: `/voice/transcribe` (multipart) — reserved, not yet wired

---

## Frontend (Checkpoint 9)

CDN React — Babel standalone, no build step, no Node.js server  
Served by Spring Boot from `src/main/resources/static/`  
Auth: `/auth/oauth2/callback` sets JWT cookie, FRONTEND_REDIRECT_URL controls post-login destination  
State: local React state — no Redux, no Zustand

---

## English Proficiency (Checkpoint 10)

`POST /api/v1/english/analyse`  
Input: transcript + context (optional)  
Output: fluencyScore, grammarScore, vocabularyScore, confidenceScore, fillerWordCount, feedback, suggestions  
All scores 1–10. Backed by AI — same AIProvider strategy.  
Error code: ENGLISH_ANALYSIS_FAILED (500)

---

## P2 Modules (in-memory, no DB table)

| Module | Endpoint prefix | Start | Next/Submit | End |
|---|---|---|---|---|
| Quiz | /api/v1/quiz | /start | /{id}/answer | /{id}/result |
| Drill | /api/v1/drill | /start | /{id}/next | /{id}/summary |
| Code Challenge | /api/v1/code | /challenge | /submit | — |

Quiz: RAPID mode only (MCQ). generateRapidQuestions() + evaluateAnswer() + extractJson() all throw on AI failure — no silent fallbacks.  
Drill: RAPID (10 Q, pre-generated) or DEEP (Socratic, 1 at a time).  
Code Challenge: generates challenge + evaluates via Judge0 if configured, AI fallback otherwise.

---

## Error Handling Pattern

All domain exceptions: `errorCode()` + `status()` accessors.  
GlobalExceptionHandler: single `@RestControllerAdvice` → `ApiError(errorCode, message, status)`.  
No stack trace, no Java class names, no "returned empty Optional" messages.  
Error codes are human-readable strings: `QUIZ_NOT_YET_COMPLETE`, `DRILL_GENERATION_FAILED`.

---

## Key Test Files

Controller tests: `@WebMvcTest` + `@Import(SecurityConfig.class)` + `@ActiveProfiles("test")` + `@MockitoBean` (Spring Boot 4.x — NOT `@MockBean`)  
Auth simulation: `UsernamePasswordAuthenticationToken(new AuthenticatedUser(...), null, List.of())`  
Applied via: `.with(authentication(authToken()))`

Service tests: plain `@ExtendWith(MockitoExtension.class)` + `@InjectMocks` + `@Mock`

---

## Parked Items (P1 unblocked by traffic)

- Kafka (EventPublisher V2) — multi-instance trigger
- Redis session store — multi-instance trigger
- Google OAuth production setup — Google Cloud Console + AUTH_MODE=oauth
- Claude + OpenAI providers — owner instruction
- Pagination on /sessions, /messages — volume trigger
- Rate limiting on P2 endpoints — public deploy trigger

---

## Deployment

Railway (backend) + Vercel (frontend) + Supabase (PostgreSQL)  
Env vars control all runtime modes — no Spring Profiles, no code change per environment  
Docker: multi-stage build (openjdk:25-slim base)  
Health: `/actuator/health` (no auth required)
