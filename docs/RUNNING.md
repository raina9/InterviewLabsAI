# Running InterviewLab — Single User (Local)

## Prerequisites
- Java 25, Maven, Docker Desktop (for PostgreSQL)
- Ollama installed with llama3.2 model: `ollama pull llama3.2`

## Start
1. Ensure Ollama is running (desktop app in background). Verify:
   ```
   curl http://127.0.0.1:11434/api/tags
   ```
2. From project root:
   ```
   .\start-local.ps1
   ```
   (loads `.env`, starts Docker PostgreSQL, runs Flyway migrations, boots Spring Boot on port 8080)
3. Open: http://localhost:8080/index.html

## First use
- Intake form is pre-filled with test defaults — submit to start an interview
- First AI call takes 30-40 seconds (Ollama model cold start) — subsequent calls are fast
- Stop with Ctrl+C

## Default configuration (zero setup needed)
| Variable | Default | Meaning |
|---|---|---|
| `DEPLOYMENT_MODE` | `personal` | Single-user mode, no production guards |
| `SESSION_STORE` | `memory` | In-memory sessions, no Redis needed |
| `AI_PROVIDER` | `ollama` | Local free AI, no API key needed |
| `AUTH_MODE` | `dev` | Dev token auth, no Google OAuth needed |

Dev API access:
```
curl -H "X-Dev-Token: dev-secret" http://localhost:8080/api/v1/auth/me
```

## Troubleshooting
- "Ollama call failed" → Ollama not running, or first-call cold start timeout — retry once
- Port 8080 busy → stop previous instance (Ctrl+C) or check for orphan Java process
- Flyway checksum error → `docker-compose down -v` (resets local DB), then restart
- Schema validation error on startup (`missing table`/`missing column`, especially right after
  `docker-compose down -v`) → a native PostgreSQL service on this machine may be bound to port
  5432 and intercepting connections meant for the Docker container (check with
  `Get-NetTCPConnection -LocalPort 5432` — if the owning process isn't Docker's backend, stop
  the native service: `Stop-Service -Name "<service-name>" -Force`, found via
  `Get-Service | Where-Object { $_.Name -match "postgres" }`)
- Migrations don't seem to run / app fails validating an empty or behind-schema database →
  Flyway's in-app autoconfiguration is intentionally disabled (`spring.flyway.enabled=false`;
  Spring Boot 4.1.0 splits Flyway/JPA autoconfiguration into separate modules whose built-in
  ordering produces a circular `flywayInitializer`/`entityManagerFactory` dependency at startup).
  Migrations run via the `flyway-maven-plugin` instead, as their own step before the app starts —
  `start-local.ps1` already does this (`mvn flyway:migrate` then `mvn spring-boot:run`); if
  running Maven goals manually, run `mvn flyway:migrate` yourself first.

## Going beyond single user
See README ["Capacity & Scaling"](../README.md#capacity--scaling) section — production mode, Redis, OAuth, and deployment are activated via environment variables only, no code changes.
