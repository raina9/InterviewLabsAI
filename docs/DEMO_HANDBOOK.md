# InterviewLab — Demo Handbook

Cheat-sheet for demoing InterviewLab and activating it for real users. Full setup detail: [RUNNING.md](RUNNING.md).

## 1. Quick Start (single user — zero config)

**Prerequisites:** Java 25, Maven, Docker Desktop, Ollama (`ollama pull llama3.2`)

**Steps:**
1. Ollama running in background (`curl http://127.0.0.1:11434/api/tags` to verify)
2. `.\start-local.ps1` — loads `.env`, starts Docker Postgres, runs Flyway, boots Spring Boot on port 1212
3. Open `http://localhost:1212/index.html`

First AI call: 30-40s (Ollama cold start), then fast.

Dev API check:
```
curl -H "X-Dev-Token: dev-secret-local" http://localhost:1212/api/v1/auth/me
```

## 2. Demo Script (5-minute walkthrough)

1. **Intake form** → role, company, JD, experience → Start Interview
2. **Phase-structured questions** — warm-up → technical → deep-dive → scenario
3. **Answer one question** → MentorAgent feedback (good / missing / refined answer / model answer + score)
4. **Psychology nudge** — shown when the mentor loop triggers one
5. **Resume upload (PDF)** → confirm it feeds interview context (next question references it)
6. **Quiz mode** → generate → answer → score
7. **Topic drill** → RAPID (10 quick Q&A) and DEEP (Socratic, 8 turns) modes
8. **Code challenge** → Monaco editor → submit → AI/Judge0 feedback
9. **Finish** → proficiency report (assessment + curriculum)

## 3. Mode Switch Table (single user → 1000 users)

| Layer | Personal (now) | Production (1k users) | Switch |
|---|---|---|---|
| Deployment | `DEPLOYMENT_MODE=personal` | `DEPLOYMENT_MODE=production` | env var, validator hard-fails if prod prereqs missing |
| Session store | `SESSION_STORE=memory` | `SESSION_STORE=redis` + `REDIS_URL=rediss://...upstash.io:6379` | env var |
| AI provider | `AI_PROVIDER=ollama` | `AI_PROVIDER=gemini` (or `claude`/`openai`) + `GEMINI_API_KEY=...` | env var |
| Auth | `AUTH_MODE=dev` + `DEV_TOKEN=dev-secret-local` | `AUTH_MODE=oauth` + `GOOGLE_CLIENT_ID=...` + `GOOGLE_CLIENT_SECRET=...` | env var |
| Rate limits | relaxed (personal mode) | enforced sliding/fixed-window per user | `DEPLOYMENT_MODE=production` |
| Database | Docker Postgres (local) | Neon or RDS — `DB_URL=jdbc:postgresql://...` | env var |
| AI budget | none | `AI_DAILY_GLOBAL_LIMIT=5000` (503 past limit) | env var |

## 4. Production Activation Checklist (in order)

1. **Redis (Upstash)** → `REDIS_URL` + `SESSION_STORE=redis`
2. **Google OAuth** → `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` + `AUTH_MODE=oauth`
3. **Paid AI** → `AI_PROVIDER=gemini|claude|openai` + matching API key
4. **`DEPLOYMENT_MODE=production`** — validator hard-fails startup if 1-3 are incomplete
5. **Deploy** — Neon (DB) + Render (app), env vars from `.env.example`
6. **GitHub secret `DB_URL`** → daily backup workflow activates
7. **UptimeRobot** → monitor `/actuator/health`

## 5. Guards Active in Production

- Dev token physically absent (`DevTokenFilter` refuses to wire up — startup fails if `DEPLOYMENT_MODE=production` and dev filter would load)
- Swagger try-it-out disabled
- AI concurrency queue — `429` + `Retry-After` under load
- Daily AI budget kill switch — `503` once `AI_DAILY_GLOBAL_LIMIT` is hit
- Redis-backed rate limiting (fixed-window)
- DB `CHECK` constraints on enum-like columns

## 6. Key Endpoints Cheat Sheet

| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `/api/v1/auth/me` | Current user profile | Required |
| POST | `/api/v1/auth/logout` | Clear JWT cookie | Required |
| POST | `/api/v1/me/resume` | Upload resume PDF (multipart) | Required |
| DELETE | `/api/v1/me` | Delete account + all data | Required |
| GET | `/api/v1/profile` | Get/create profile | Required |
| PUT | `/api/v1/profile` | Update profile metadata | Required |
| PUT | `/api/v1/profile/resume` | Update resume text | Required |
| PUT | `/api/v1/profile/custom-prompt` | Update custom prompt | Required |
| POST | `/api/v1/sessions` | Create session | Required |
| GET | `/api/v1/sessions` | List sessions | Required |
| GET | `/api/v1/sessions/{id}` | Get session | Required |
| POST | `/api/v1/sessions/{id}/complete` | Complete session | Required |
| POST | `/api/v1/sessions/{id}/abandon` | Abandon session | Required |
| GET | `/api/v1/sessions/{id}/messages` | Session transcript | Required |
| POST | `/api/v1/interview/start` | Start interview, get first question | Required |
| POST | `/api/v1/interview/{sessionId}/respond` | Submit answer, get next Q + feedback | Required |
| GET | `/api/v1/interview/{sessionId}/feedback` | All mentor feedback for session | Required |
| POST | `/api/v1/voice/transcript` | Submit voice transcript (voiceUsed=true) | Required |
| POST | `/api/v1/english/analyze` | English proficiency analysis | Required |
| POST | `/api/v1/quiz/start` | Start quiz | None |
| POST | `/api/v1/quiz/{sessionId}/answer` | Submit quiz answer | None |
| GET | `/api/v1/quiz/{sessionId}/result` | Quiz result | None |
| POST | `/api/v1/drill/start` | Start topic drill (RAPID/DEEP) | None |
| POST | `/api/v1/drill/{sessionId}/next` | Submit drill answer, next question | None |
| GET | `/api/v1/drill/{sessionId}/summary` | Drill summary — weak/strong spots | None |
| POST | `/api/v1/code/challenge` | Generate code challenge | None |
| POST | `/api/v1/code/submit` | Submit code solution | None |
| GET | `/api/v1/code/challenge/{id}/hint` | Get hint | None |
| POST | `/api/v1/assessment/start` | Start self-assessment | Required |
| POST | `/api/v1/assessment/submit` | Submit topic ratings | Required |
| GET | `/api/v1/assessment/report/{userId}` | Proficiency report | Required (self only) |
| GET | `/api/v1/curriculum/{userId}` | Personalised learning plan | Required (self only) |
| GET | `/api/v1/admin/stats` | Platform usage stats | Required (ADMIN role) |
| PATCH | `/api/v1/admin/system-feedback/{id}/applied` | Toggle feedback applied flag | Required (ADMIN role) |
| GET | `/actuator/health` | Health check | None |

## 7. Troubleshooting (top 5)

1. **Ollama cold start / not running** — first call takes 30-40s; if it fails, verify `curl http://127.0.0.1:11434/api/tags`, retry once
2. **Port 1212 busy** — stop previous instance (Ctrl+C) or kill orphan Java process
3. **Native Windows Postgres intercepting 5432** — check `Get-NetTCPConnection -LocalPort 5432`; if owner isn't Docker, `Stop-Service` the native Postgres service
4. **Flyway on fresh DB** — migrations run via `flyway-maven-plugin`, not Spring Boot autoconfig (`spring.flyway.enabled=false` — see RUNNING.md); `start-local.ps1` runs `mvn flyway:migrate` before boot automatically
5. **401 with dev token** — `AUTH_MODE=dev` always wins over OAuth auto-detection now (fixed), but confirm `.env` has `AUTH_MODE=dev` and `DEV_TOKEN=dev-secret-local` set explicitly

## 8. Capacity — Honest Numbers

| Tier | Cost | Notes |
|---|---|---|
| Personal | $0 | 1 user, Ollama local, Docker Postgres |
| Pilot | ~$0-7/mo | Free tiers — Upstash Redis, Neon Postgres, Render free/starter |
| Production 1k-10k users | ~$50-60/mo + AI usage | AI spend capped by `AI_DAILY_GLOBAL_LIMIT` kill switch |

No load test has been run yet — get a k6 report before making any go-live capacity claims.
