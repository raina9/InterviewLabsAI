# Changelog

Version history reconstructed from checkpoint history (`CLAUDE.md` → `CURRENT CHECKPOINT`), session notes, and commit history. Versions are functional milestones, not semver releases against a published package.

## V1.0 — Core Interview Flow (Checkpoints 1-9)
- Auth layer: Google OAuth2 + JWT, dev-token fallback for local development
- Schema + Flyway migrations: `users`, `user_profiles`, `sessions`, `messages`, `answer_feedback`, `proficiency`, `system_feedback`
- Service + REST API layer across core domains
- AI provider layer: Strategy pattern (`AiProviderStrategy`), Gemini as the original active provider
- Agent core: `InterviewAgent` / `MentorAgent` separation, `AgentToolChain` (Chain of Responsibility, 5 tools)
- Interview flow V1: session creation, turn-by-turn Q&A, mentor feedback loop
- Voice support: transcript-based voice input
- Frontend CDN V1: React served via CDN + in-browser Babel, no build step — [[decisions/ADR-003-cdn-react-over-bundled]]
- Commit: `a4cd7c7` — Initial release

## V1.1 — English Proficiency Module (Checkpoint 10)
- `EnglishController` / `EnglishService`: transcript analysis for fluency, grammar, vocabulary, filler words
- Commit: `b601847` (per CLAUDE.md checkpoint record)

## V1.2 — P1 Features: Assessment, Curriculum, Psychology
- Self-rating assessment intake → proficiency report (`AssessmentController`, `AssessmentService`)
- AI-generated personalised curriculum from the assessment report (`CurriculumController`, `CurriculumService`)
- Psychology nudges: pattern detection every 3rd answer (NERVOUS / IMPROVING / SOLID / OVERCONFIDENT) surfaced in-session
- OAuth SOP documented; OAuth auto-detect added (switches to `oauth` mode automatically when Google credentials are present, regardless of `AUTH_MODE`)
- Mentor feedback surfaced directly in chat; voice integration expanded
- Commits: `d894111` — mentor feedback in chat, voice integration, OAuth auto-detect; `0983d36` — assessment report, curriculum generator, psychology nudges, OAuth SOP

## V1.3 — P2 Features: Quiz, Code Challenge, Drill, Proficiency UI
- `QuizController`/`QuizService`: MCQ quiz generation and scoring
- `CodeChallengeController`/`CodeChallengeService`: coding challenge generation, submission evaluation
- `DrillController`/`DrillService`: RAPID (10 Q&A) and DEEP (Socratic) topic drilling
- Proficiency UI, mobile voice support
- Commit: `71b655f` — Add quiz mode, code challenge, topic drill, proficiency UI, mobile voice

## V1.4 — Standards Audit + Swappable Backend Pattern
- Critical audit findings fixed: error codes standardized, data corruption issues resolved, missing test coverage added, documentation gaps closed
- Swappable Backend Pattern established consistently across AI provider, messaging (`EventPublisher`), storage (`StorageService`), and agent orchestration (`AgentOrchestrator`) — [[decisions/ADR-004-conditional-on-property-pattern]]
- **AI provider default switched from Gemini to Ollama** — [[decisions/ADR-001-ollama-over-gemini]]
- AI config wired fully into agent services; timeouts and token/temperature limits externalized to `AiProperties` (`app.ai.*`) — no hardcoded values in agent classes
- Known limitations documented explicitly rather than left implicit (see [[mentorship/weak-areas]] for the running list)
- Commits: `ef6f104` — Fix critical audit findings (error codes, data corruption, missing tests, docs); `4450acd` — Fix critical audit findings, establish swappable backend pattern across AI, messaging, storage, agent orchestration; `a405f93` — Wire AI config into agent services, externalize timeouts and limits, document known limitations

## V1.5 — GitHub Release + ForgeKit
- Initial open source release: `github.com/raina9/InterviewLabsAI` — 154 files, clean history, no secrets, no session notes
- Excluded from public repo: `.env`, `CLAUDE.md`, `session-notes/`, `.claude/`, `target/`
- Seed data scrubbed to placeholders only (`dev@interviewlab.local`) — see CLAUDE.md `OPEN SOURCE RELEASE STANDARD`
- Internal roadmap removed from public README
- ForgeKit available as an on-demand documentation tool (trigger keyword: `forgekit`), zero token cost unless invoked
- This documentation layer added: ADRs (`docs/decisions/`), mentorship docs (`docs/mentorship/`), system design, mastery tracker, authz matrix, AI architecture doc
- Commit: `42d4334` — Remove internal roadmap from public README; git release log entry: 2026-06-28

## V1.6 — Scale-Ready: SessionStore/Redis, AI Queue, DB Constraints, Admin Stats
- `SessionStore` abstraction (`sessionstore/` package) + `DeploymentModeValidator` — `InMemorySessionStore` default, `RedisSessionStore` opt-in via `SESSION_STORE=redis` + `REDIS_URL`, zero-config for local dev — [[decisions/ADR-009-sessionstore-abstraction]]
- `AIRequestQueue` (Semaphore-based) + `AiBudgetGuard` daily kill switch — protects against a runaway AI bill independent of per-user rate limiting — [[decisions/ADR-011-ai-request-queue]]
- DB integrity: `V10__add_check_constraints.sql` — CHECK constraints added at the DB level; GDPR `DELETE /me` account-deletion endpoint (`UserController`/`UserAccountService`); Monaco editor lazy-loaded; a11y fixes; capacity docs
- Admin stats endpoint (`AdminController`/`AdminStatsService`), scheduled DB backup GitHub Actions workflow, Prometheus/Micrometer metrics wiring
- Commits: `23de362` — SessionStore abstraction + DEPLOYMENT_MODE validator; `71f3bf7` — AI request queue + daily budget kill switch + production security hardening; `2382654` — DB integrity constraints, GDPR delete endpoint, Monaco lazy load, a11y fixes, capacity docs; `715235e` — Admin stats endpoint, DB backup workflow, Prometheus metrics wiring

## Documentation Decision — `docs/system-design.md` vs `docs/hld.md`
`docs/system-design.md` was evaluated as a possible new doc during a documentation-completion
pass and deliberately **not** duplicated into a second file — at V1.6 scale it substantially
overlaps `hld.md` (component breakdown, data flow) while adding capacity estimation, deep-dives,
and the "at 10x scale" trade-off analysis that `hld.md` intentionally keeps out of its own scope.
Decision: keep both, but `hld.md` only gets a short capacity pointer (see `hld.md` → "Capacity &
Scale (short)") rather than restating the numbers — `system-design.md` remains the single source
for capacity/scale material. Re-evaluate if the two docs drift further or a reader consistently
needs both open at once.

See also: [[decisions/ADR-001-ollama-over-gemini]] through [[decisions/ADR-011-ai-request-queue]], [[mentorship/mistakes-and-fixes]]
