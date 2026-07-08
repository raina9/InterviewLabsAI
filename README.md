# Interview Lab

[![Open Source](https://img.shields.io/badge/Open%20Source-Yes-brightgreen)](https://opensource.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Built with Spring AI + Ollama](https://img.shields.io/badge/Built%20with-Spring%20AI%20%2B%20Ollama-6DB33F)](https://spring.io/projects/spring-ai)
[![Java 25 LTS](https://img.shields.io/badge/Java-25%20LTS-007396)](https://openjdk.org/)
[![Topics](https://img.shields.io/badge/topics-java%20%7C%20spring--boot%20%7C%20ai%20%7C%20interview--prep%20%7C%20ollama%20%7C%20spring--ai%20%7C%20open--source%20%7C%20mentorship%20%7C%20llm-blue)](https://github.com/topics/)

AI-powered interview mentorship engine. Context-aware mock interviews with an agentic mentor loop — personalised feedback, refined answers, model answers, and psychology notes. Candidate profile evolves across sessions.

## What This Is

Interview Lab is a **mentorship engine**, not a quiz tool.

You bring your resume, job description, and experience level. The system generates context-aware interview questions, evaluates your answers, and returns:

- **Feedback** — what was missing or weak
- **Refined answer** — your answer improved in structure and depth
- **Model answer** — an ideal, self-contained response written for interviewer psychology
- **Psychology note** — why interviewers react to this answer style

Every session builds on the last. Your proficiency profile evolves over time.

## Stack

| Layer          | Technology                                                      |
|----------------|-----------------------------------------------------------------|
| Language       | Java 25 (LTS) — Virtual threads, Records, Sealed interfaces     |
| Framework      | Spring Boot 4.1.0 — MVC, Security, Data JPA                     |
| AI             | Gemini V1 — Strategy pattern, provider-swappable via env var    |
| Auth           | Google OAuth2 + JWT (JJWT 0.12.x)                              |
| Database       | PostgreSQL via Supabase (prod) / Docker (local)                 |
| Migrations     | Flyway — versioned SQL, no auto-ddl                             |
| Messaging      | Spring Kafka — Upstash (prod) / Docker (local)                  |
| API Docs       | Springdoc OpenAPI — Swagger UI at `/swagger-ui.html`            |
| Observability  | Spring Actuator + structured logging with MDC correlation ID    |
| Hosting        | Railway (backend) + Vercel (frontend) + Supabase (database)     |

## Architecture

```
InterviewAgent   — question generation + session flow ONLY
MentorAgent      — feedback + refined answer + model answer + psychology note ONLY

AgentTools chain (Chain of Responsibility):
  ResumeContextTool → SessionHistoryTool → ProficiencyTool → QnAReferenceTool → UserPromptTool
```

## Architecture: Swappable Backends

Every external dependency (AI provider, storage, messaging, agent orchestration) runs on free or local infrastructure by default, and switches to a paid or cloud provider via a single environment variable — no code changes, no recompile, no redeployment of a different binary required. This is the same conditional bean mechanism Spring Boot uses internally for its own auto-configuration. The paid implementation code lives in the repository in a conditional, non-instantiated state; enabling it is an ops action, not a dev action.

## Quick Start

Zero-config single-user local run (Ollama, in-memory sessions, dev auth — no API keys, no OAuth setup): see [docs/RUNNING.md](docs/RUNNING.md).

## How to Run Locally

The steps below set up the full stack with Google OAuth2 and a hosted AI provider (Gemini) — useful when you want production-equivalent auth and a cloud AI provider locally. For the fastest zero-config path, use [docs/RUNNING.md](docs/RUNNING.md) instead.

### Prerequisites

- Java 25+
- Maven 3.9+
- Docker + Docker Compose
- Google OAuth2 client (Google Cloud Console)
- Gemini API key (Google AI Studio)

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env — fill in JWT_SECRET, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GEMINI_API_KEY
```

Generate JWT secret:
```bash
openssl rand -base64 64
```

### 2. Start infrastructure

```bash
docker-compose up -d
```

Verify PostgreSQL is healthy:
```bash
docker-compose ps
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Or in IntelliJ IDEA: right-click `InterviewLabApplication.java` → Run.

### 4. Verify

| Endpoint | URL |
|----------|-----|
| API base | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI spec | http://localhost:8080/api-docs |
| Health check | http://localhost:8080/actuator/health |

## Project Structure

```
src/main/java/com/interviewlab/
├── InterviewLabApplication.java  — entry point, @SpringBootApplication
├── auth/          — Google OAuth2 flow, JWT issuance + validation, security filter chain
├── ai/            — AIProvider sealed interface, AIProviderFactory, GeminiAIProvider
├── agent/         — InterviewAgent, MentorAgent, AgentTools chain (CoR pattern)
├── session/       — Session lifecycle, message flow (INTERVIEWER/CANDIDATE roles)
├── profile/       — User profile, resume text, custom prompts, cross-session state
├── feedback/      — Answer feedback (MentorAgent output), system feedback (ratings)
├── proficiency/   — English + domain proficiency scores (evolve per session)
├── event/         — Spring ApplicationEvents V1 → Kafka V2 (parked, plug-in ready)
└── config/        — Security, CORS, RestClient, OpenAPI, Kafka, JWT config beans

src/main/resources/
├── application.yml          — all configuration, env-var driven
└── db/migration/            — Flyway SQL scripts (V{n}__{description}.sql)
```

## Database Migrations

All schema changes go in `src/main/resources/db/migration/`.

Naming convention: `V{n}__{description}.sql`

Examples:
- `V1__create_users.sql`
- `V2__create_sessions_and_messages.sql`
- `V3__create_answer_feedback.sql`

Never modify a migration file after it has been applied to any environment.

## Environment Variables

See [.env.example](.env.example) for the full reference with descriptions.

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_URL` | Yes | `jdbc:postgresql://localhost:5432/interviewlab` | PostgreSQL connection URL |
| `DB_USERNAME` | Yes | `interviewlab` | Database username |
| `DB_PASSWORD` | Yes | `interviewlab` | Database password |
| `JWT_SECRET` | **Yes** | — | Base64-encoded 64-byte secret. No default — app fails without this. |
| `GOOGLE_CLIENT_ID` | **Yes** | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | **Yes** | — | Google OAuth2 client secret |
| `GEMINI_API_KEY` | **Yes** | — | Gemini API key (Google AI Studio) |
| `KAFKA_BOOTSTRAP_SERVERS` | No | `localhost:9092` | Kafka broker address |
| `DAILY_LIMIT` | No | `60` | Max interview interactions per user per day |
| `DEPLOYMENT_MODE` | No | `personal` | `personal` \| `public` \| `embedded` |
| `AI_PROVIDER` | No | `gemini` | Active AI provider (Strategy pattern — swap without code change) |

## Deployment

| Target | Platform | Notes |
|--------|----------|-------|
| Backend | Railway | Set all env vars from `.env.example` in Railway service |
| Frontend | Vercel | CDN-delivered React (no build step in V1) |
| Database | Supabase | Use Transaction pooler URL for Railway compatibility |
| Kafka | Upstash | Set `KAFKA_SECURITY_PROTOCOL=SASL_SSL` + SASL credentials |

## Docs

- `docs/` — HLD, LLD, System Design, API Contracts, DB Schema (added per checkpoint)
- `session-notes/` — Session-by-session development log

## Known Limitations

- **In-memory session store is the default** — Quiz, Topic Drill, and Code Challenge sessions (and rate-limit counters) live in a `SessionStore` abstraction that defaults to `ConcurrentHashMap` (`SESSION_STORE=memory`). A single-instance deployment loses active sessions on restart; multi-instance deployments will drop sessions that land on a different instance. Set `SESSION_STORE=redis` + `REDIS_URL` to switch to the Redis-backed implementation — no code change required. See [Capacity & Scaling](#capacity--scaling).
- **Google OAuth (dev mode)** — The default `AUTH_MODE=dev` bypasses Google login entirely. See [Setting Up Google OAuth](#setting-up-google-oauth-production-mode) for switching to production auth before exposing the app publicly.
- **Quiz/Drill/Code Challenge sessions aren't user-scoped** — these ephemeral sessions carry no owner/userId check; anyone authenticated who knows a session ID can access it. Low risk at current scale (single-user/personal deployments), but worth knowing before exposing the app to multiple untrusted users.

## Capacity & Scaling

Honest numbers, not aspirational ones — read this before deciding how far to push a deployment.

| Mode | Topology | Session store | AI provider | Fit |
|------|----------|----------------|-------------|-----|
| **personal** (default) | Single instance | In-memory (`SESSION_STORE=memory`) | Ollama (local) | Designed for 1 user. Works fine for small demos and local dev — not for concurrent traffic. |
| **production** | N instances behind a load balancer | Redis (`SESSION_STORE=redis` + `REDIS_URL` required — `DeploymentModeValidator` refuses to start without it) | Hosted provider recommended (Gemini/Claude/OpenAI) | Stateless once Redis-backed — horizontal scaling is supported, not aspirational. |

- **Redis / Upstash free tier** — Upstash's free tier is roughly 10k commands/day. Fine for a demo or pilot; a real user base (1,000+ active users) will need a paid Redis tier (Upstash paid or self-hosted), roughly **~$10/month** at that scale. Fixed-window rate limiting (see ADR-010) deliberately uses one command per request specifically to stretch the free tier as far as possible.
- **Ollama** — single-process, local, zero cost. Fine for exactly the "1 user, personal mode" case it's designed for. Any concurrent load (more than a couple of simultaneous requests) needs `AI_PROVIDER` switched to a hosted provider (Gemini is the active paid-provider integration) — Ollama does not autoscale.
- **AI daily budget kill switch** — `AI_DAILY_GLOBAL_LIMIT` (default 1000 calls/day, see ADR-011) caps total AI provider calls across every user and every provider, independent of per-user rate limiting. This is a runaway-cost circuit breaker, not a capacity feature — raise it deliberately, not as a reflex fix for hitting the limit.
- **Horizontal scaling** — the app is stateless once `SESSION_STORE=redis` is active: N instances behind a load balancer is a supported topology, not a future roadmap item. `DeploymentModeValidator` enforces this precondition at startup in `DEPLOYMENT_MODE=production` (fails fast rather than silently running multi-instance on in-memory state).

## Contributing

Contributions welcome — open a PR or issue.

## Setting Up Google OAuth (Production Mode)

By default the app starts without Google OAuth (dev mode — no login required). To switch to production OAuth:

1. Go to [console.cloud.google.com](https://console.cloud.google.com) → APIs & Services → Credentials
2. Create an **OAuth 2.0 Client ID** (Web application type)
3. Add an Authorised Redirect URI: `https://<your-backend-url>/login/oauth2/code/google`
4. Copy the Client ID and Client Secret into your `.env`:
   ```
   GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your-client-secret
   ```
5. Restart the app — it auto-detects the credentials and switches to OAuth mode. No `AUTH_MODE` flag needed.

## Author

Shivendra Mohan Raina
