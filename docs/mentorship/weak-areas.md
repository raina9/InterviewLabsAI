---
name: weak-areas
---

# Weak Areas

Honest gap list — things likely to come up in a Senior/Staff Backend or AI Architect interview that InterviewLab's codebase does **not** currently demonstrate hands-on. Ranked by how likely they are to be probed given the target role (CLAUDE.md `WHO`).

## HIGH RISK — will be asked in interview

| Area | Current State | Why It's Risky |
|---|---|---|
| Kafka internals | Seam built (`EventPublisher` interface, `docker-compose.yml` KRaft broker, `application.yml` producer/consumer config), but `KafkaEventPublisher` is not implemented — `SyncEventPublisher` is the only active implementation | Can describe the intended design (delivery guarantees config, idempotent producer, `read_committed` isolation) but has never run a real consumer group, handled rebalancing, or debugged consumer lag against a live broker |
| Two-Phase Commit (2PC) / distributed transactions | Not implemented anywhere in the codebase | Monolith + single Postgres instance means no distributed transaction has ever been needed — this is a real gap for any question probing distributed consistency beyond "eventual consistency, in theory" |
| DSA (Data Structures & Algorithms) | Not practiced as part of this project — CLAUDE.md `SKILLS` self-rates DSA 3/10 | Staff/Architect-track interviews still gate on DSA rounds at most companies; this needs a dedicated practice track outside InterviewLab itself (see DSA template in workspace CLAUDE.md) |
| Cursor-based pagination | Not implemented — `SessionController.GET /api/v1/sessions` list endpoint uses standard offset pagination (Spring Data `Pageable`), not cursor/keyset pagination | Cursor pagination is a common System Design deep-dive question (why offset pagination degrades at scale); no hands-on implementation to point to |
| Circuit breaker / Resilience4j | Parked — retry/timeout exist at the HTTP client level (`AI_REQUEST_TIMEOUT_SECONDS`), but no circuit breaker, no bulkhead, no fallback chain | Reliability patterns (CLAUDE.md `100 ENGINEERING PARAMETERS` → Reliability) are a named requirement but not yet demonstrated in running code |

## MEDIUM RISK

| Area | Current State | Why It's Risky |
|---|---|---|
| Redis | Parked — in-memory single-instance stores are used for quiz/drill/code session state (see CLAUDE.md `PARKED ITEMS`: "Redis-backed session store") | Caching strategy questions (cache-aside, invalidation, TTL design) have no running implementation to reference, only the stated intent |
| pgvector / RAG | Not implemented — `nomic-embed-text` + pgvector are named in the workspace stack but not yet wired into InterviewLab | AI Architect track explicitly requires RAG competence; this is the single biggest gap relative to the "AI Architect" target role |
| Distributed tracing | Partial — structured logging with correlation/trace/request IDs is a stated non-negotiable (CLAUDE.md `100 ENGINEERING PARAMETERS`), but no OpenTelemetry collector or trace visualization has been wired up end to end | Can describe the intended shape but hasn't debugged a real trace waterfall across services |

## Not a gap, but worth naming explicitly
There is currently **no role-based access model** (no `ADMIN` role exists anywhere in the codebase — see `docs/authz-matrix.md`). Every authenticated user has identical access to every endpoint. This is fine for a single-user-per-account V1 product, but it means authorization-design questions ("how would you model admin vs candidate permissions") have no running code to point to, only a design answer.

See also: [[concepts-mastered]], [[../mastery-tracker]], [[mistakes-and-fixes]]
