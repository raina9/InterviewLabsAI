# InterviewLab — Engineering Learning Curriculum

Absorbed from the retired HireFlowAI POC (`docs/HIREFLOW_ALL_PHASES.md`, generated 2026-07-02).
HireFlowAI was retired 2026-07 — see workspace CLAUDE.md. All transferable engineering
concepts, design decisions, interview talking points, and NEVER DO THIS rules from that
document are preserved here, remapped to InterviewLab's own architecture. Entity-specific
material (Company/Job/Application, MySQL-only specifics) has been stripped; only concepts
that transfer to any domain remain.

This is a **living document** — patch sections in place as phases complete or status
changes. Never regenerate wholesale.

---

## HOW TO USE THIS DOCUMENT

Each phase below is a self-contained engineering topic: scope, design decisions (with
rejected alternatives and trade-offs), interview talking points (including one trap
question per phase), NEVER DO THIS rules, and a Definition of Done template. The status
line under each phase heading tracks where InterviewLab actually stands against that
topic — some phases are done, some are seams waiting to be activated, some are parked.

---

## PHASE 1 — Monolith CRUD

**Status: DONE — InterviewLab core (sessions, messages, answer_feedback, proficiency
entities; full REST CRUD under `/api/v1/`)**

### Scope (general pattern)
A layered monolith: entity → repository → service → controller, with pagination
(`Pageable`/`Page<T>`, 0-indexed, capped page size), sorting, filtering, Jakarta Bean
Validation, a structured error envelope via `@RestControllerAdvice`, MDC correlation-ID
request logging, and Flyway-managed schema (`ddl-auto: validate`, never `update`).

### Design decisions
- **Soft delete only where audit/compliance weight exists**, not on every entity.
  Rejected: hard-delete everywhere, or soft-delete everywhere. Entities that carry a
  record of "this happened" (an interview session, a scored answer) survive downstream
  state changes; purely operator-managed data doesn't need it. Trade-off: every query
  against a soft-deleted entity must remember the `deleted_at IS NULL` filter.
- **Cross-field validation lives in the service layer**, not a DB `CHECK` constraint or
  bean-validation annotation alone. Per-field annotations can't express a relationship
  between two fields; a DB constraint enforces it but returns an unreadable SQL error
  instead of a clean 400. Trade-off: any future direct-SQL write path must reimplement
  the rule.
- **Flexible/schemaless columns (JSON) over a join table** when there's no current query
  requirement against the individual elements. Revisit when a real filtering/search
  requirement appears — at that point a proper index or a semantic-search layer (Phase 9)
  replaces the relational need entirely rather than bolting on a join table.
  Trade-off: a JSON column without a generated/indexed projection doesn't scale to
  predicate queries; accepted with a known escape hatch.
- **Config-driven defaults, not DB `DEFAULT` clauses**, for any value that might vary by
  environment. A DB default is invisible to the app/tests and needs a migration to
  change; an `application.yml`-bound default is explicit, testable, and
  environment-overridable. Trade-off: raw SQL inserts bypassing the service layer will
  fail `NOT NULL` — by design, the service layer must never be bypassed.
- **Uniqueness under soft delete needs more than a naive composite unique index.**
  This is the `active_dedup_key` story, generalized: a naive
  `UNIQUE(candidate_key, deleted_at)` composite silently fails in databases (MySQL among
  them) where NULL is treated as distinct from NULL in unique indexes — two "active"
  (non-deleted) rows with the same logical key can both get `deleted_at = NULL` and the
  index never fires. **MySQL's idiomatic fix**: a `VIRTUAL GENERATED` column that
  collapses active rows to a real deterministic key (enforced by a unique index) and
  deleted rows to `NULL` (exempt). **PostgreSQL's equivalent — the pattern InterviewLab
  would actually use**: a partial unique index —
  `CREATE UNIQUE INDEX ux_active_key ON table(candidate_key) WHERE deleted_at IS NULL`.
  Postgres's partial index is the cleaner primitive; MySQL's generated-column trick
  exists because MySQL has no native partial-index equivalent. This is a strong general
  interview talking point on database-specific constraint modeling.

### Interview talking points
- **Q: Why start with a monolith?** Establish correct domain boundaries and API
  contracts before distributing them. A well-understood boundary decomposes cleanly
  later (Phase 2); a poorly understood one is expensive to split.
- **Q: Why Flyway instead of `ddl-auto=update`?** `update` adds columns but never
  removes/reorders them and has no rollback; Flyway gives versioned, auditable,
  rollback-capable migrations that go through review.
- **Q: What's the trade-off of a JSON/flexible column vs a relational join table?**
  Flexible schema and simple writes, at the cost of no efficient SQL predicate on
  individual elements — acceptable only until a real filtering/search requirement shows
  up, at which point indexing or semantic search (not a join table) is usually the
  better next step.
- **[TRAP] Q: If `open-in-view` is `false` and code outside the service layer accesses
  a lazy association after the service method returns, what happens?** A
  `LazyInitializationException` — the Hibernate session is already closed. People who've
  only read that the flag means "slightly slower" are surprised that it's actually a
  hard boundary forcing all lazy access into the service layer. Fix: fetch eagerly or map
  to a DTO while the session is still open — never flip the flag back to `true`.
- **Q: Why does a naive `UNIQUE(key, deleted_at)` fail to prevent duplicate active
  rows?** NULL-distinctness — most databases treat every NULL as unique from every other
  NULL in a unique index, so two soft-deleted-as-NULL "active" rows both satisfy the
  constraint. Postgres solves this natively with a partial unique index
  (`WHERE deleted_at IS NULL`); MySQL needs the generated-column workaround because it
  has no partial index primitive.

### Never do this
- Never edit a committed Flyway migration — write a new versioned one instead.
- Never assume a DB default exists for a value the service layer is responsible for
  applying — verify the config path, not the schema.
- Never treat `UNIQUE(key, deleted_at)` as sufficient duplicate-prevention on any
  database where NULL-distinctness applies — verify which uniqueness primitive
  (partial index vs generated column) your database actually needs.
- Never let `ddl-auto` do anything but `validate` — Flyway owns schema, always.
- Never expose stack traces, class names, or "empty Optional" language in API error
  messages — error responses are consumer-facing only.

### Definition of done
- [x] Layered CRUD complete with pagination/sorting/filtering
- [x] Structured exception hierarchy + `@RestControllerAdvice`
- [x] Flyway-managed schema, `ddl-auto: validate`
- [x] MDC correlation-ID logging
- [x] Tests (unit + slice) passing
- [x] Docs updated (hld.md, api-contract.md, db-schema.md)

---

## PHASE 2 — Microservices

**Status: PARKED — monolith-first per ADR-002; boundaries kept extractable**

### Scope (general pattern)
Decompose a proven monolith along already-correct domain boundaries into independently
deployable services, plus a discovery service and an API gateway that preserves the
external contract while routing internally.

### Design decisions
- **Split by domain ownership, not technical layer.** Splitting by layer (all
  controllers in one service, all repositories in another) recreates a distributed
  monolith. Split along boundaries already proven correct by the monolith's FK/cascade
  rules — each service gets its own database. Trade-off: cross-entity queries that were
  a single SQL join become a network call or a read-model.
- **Declarative service-to-service clients (e.g. Feign) over raw HTTP clients**, for
  parity with the team's existing Spring-first skill level and out-of-the-box discovery
  integration — not gRPC, which adds a second serialization stack with no clear
  near-term requirement. Trade-off: a blocking client model ties up a thread per call;
  acceptable at moderate scale, revisited if virtual threads or a reactive migration
  changes the calculus.
- **Service discovery (e.g. Eureka) over hardcoded URLs or a full service mesh.**
  Hardcoded URLs break the moment a service scales past one instance; a mesh (Istio/
  Linkerd) is real operational overhead with no near-term payoff (no mTLS/traffic-shaping
  requirement). Trade-off: an unclustered discovery server is a single point of failure —
  acceptable for local/learning setup, not for production without addressing (often
  superseded by the orchestrator's own service discovery once containerized).
- **Gateway owns cross-cutting concerns** (correlation ID minting, rate-limit stubs) —
  a request enters the system exactly once at the gateway, which is the single correct
  place to mint it; downstream services forward, never regenerate.
- **No shared database across services**, even temporarily, even on one physical
  instance — a shared schema is a hidden coupling that defeats the point of the split.
  Trade-off: cross-schema joins become impossible; every cross-entity read now needs a
  network call.

### Interview talking points
- **Q: Why split into microservices at all, if the monolith already worked?**
  Decomposing an already-correct model along proven boundaries is fundamentally
  different from guessing at boundaries under distributed-systems pressure from day one.
- **Q: How does one service validate a foreign reference without a shared database?**
  A declarative client call resolved through service discovery — what used to be a
  `FOREIGN KEY` constraint becomes an explicit network call with its own failure mode.
- **[TRAP] Q: If a downstream service is momentarily unreachable, what does a naive
  synchronous client call do by default?** Nothing protective — most declarative HTTP
  clients (Feign included) have no retry or circuit breaker configured out of the box;
  a naive call throws a connection exception or hangs until a TCP-level timeout. People
  assume resilience is "built in"; it isn't — that's exactly what Phase 4's resilience
  patterns exist to add.

### Never do this
- Never let two services share one database schema "just for now."
- Never call a remote service client without an explicit timeout configured.
- Never register a broader gateway route predicate before a more specific one sharing a
  path prefix — the specific route becomes unreachable.
- Never assume a cross-service call has the same failure characteristics as an
  in-process method call — every remote call needs an explicit fail path.
- Never duplicate business validation logic independently across two services — keep it
  owned by exactly one service and call it.

### Definition of done (when unparked)
- [ ] Domain services extracted with independent schemas and discovery registration
- [ ] Gateway preserves the external API contract
- [ ] Contract/integration test proving gateway routing precedence
- [ ] Manually verified clean failure (not a hang) when a dependency is down

---

## PHASE 3 — Kafka / Async Messaging

**Status: SEAM READY — `EventPublisher` abstraction in place (sync V1), `MESSAGING_MODE=kafka`
switch pending (see ADR in Swappable Backend Pattern, InterviewLab CLAUDE.md)**

### Scope (general pattern)
Introduce asynchronous event flow for side effects that shouldn't sit on the critical
path of the triggering request — notification delivery, cache eviction, cross-service
fan-out.

### Design decisions
- **Async messaging over direct synchronous calls for non-critical-path side effects.**
  A side effect (e.g. notification delivery) isn't part of "accept this state change" —
  a synchronous call would make the primary action fail if the side-effect path is
  slow/down. Trade-off: eventual consistency — a consumer may process an event some time
  after the producing transaction commits.
- **At-least-once delivery with idempotent consumers, not exactly-once semantics.**
  Exactly-once adds real operational complexity for a requirement ("never double-process
  harmfully") that idempotent consumer logic (dedup by event ID) solves more simply.
  Trade-off: the consumer must maintain its own dedup state (a processed-event-ID store
  with a TTL).
- **Per-domain topics, not one shared events topic.** Separate topics allow independent
  partitioning, retention, and schema evolution; a shared topic forces every consumer to
  filter noise and couples unrelated schema changes. Trade-off: more topics to manage.
- **Staged retry topics + DLQ, not in-place retry loops.** An in-place retry blocks the
  partition behind the failing message (head-of-line blocking); staged retry topics let
  the consumer move on and reprocess on a delay, landing in a DLQ for manual inspection
  as the final stop. Trade-off: more topics and consumer-routing wiring.
- **Schema registry with enforced compatibility mode, not raw unstructured JSON.**
  Catches a breaking schema change at CI/registration time instead of in production when
  an old consumer chokes on an unrecognized field.

### Interview talking points
- **Q: Why introduce async messaging instead of keeping everything synchronous?**
  Side-effect logic doesn't belong on the critical path of the primary state-change
  request — a slow/down downstream consumer shouldn't fail the primary action.
- **Q: How do you guarantee a consumer doesn't process the same event twice?** Every
  event carries a UUID; the consumer checks it against a dedup store before processing
  and records it after — this is what "idempotent consumer" means concretely. The
  messaging platform's at-least-once delivery is exactly why this is necessary.
- **[TRAP] Q: If a consumer throws mid-processing after already partially applying a
  side effect, what does the platform's retry actually redeliver?** The entire event
  from the last committed offset — the platform has no concept of "half applied" inside
  consumer business logic. People assume the platform handles transactional partial
  state; it doesn't — the consumer logic itself must be safe to reprocess from scratch.

### Never do this
- Never retry a failed message in-place inside the consumer loop — route to a staged
  retry topic instead.
- Never assume producer-side idempotence (e.g. `enable.idempotence`) makes the
  *consumer* idempotent too — they solve different problems.
- Never key events by a random/unique field when per-entity ordering matters — key by
  the entity ID.
- Never let a schema change go out without checking registry compatibility mode first.
- Never treat a DLQ as "fire and forget" with no monitoring — that's silent data loss
  waiting to be discovered.

### Definition of done (when `MESSAGING_MODE=kafka` unparked)
- [ ] Producer(s) on the relevant domain event(s), consumer with idempotent dedup
- [ ] Retry-topic routing + DLQ wiring
- [ ] Idempotency test: forced consumer crash-and-restart produces zero duplicate
      side effects

---

## PHASE 4 — Concurrency

**Status: PARTIAL — `AIRequestQueue` (Semaphore-based, ADR-011) done for AI-call
concurrency control; optimistic/pessimistic locking on mutable entities not yet needed
at current scale**

### Scope (general pattern)
Address concurrent-write correctness gaps and adopt virtual threads for I/O-bound
request handling.

### Design decisions
- **Optimistic locking (`@Version`) as the default for low-contention entities.**
  Concurrent edits to the same row by two different actors are rare in practice;
  optimistic locking has zero cost on the uncontended path and only produces a conflict
  exception in the rare collision case — pessimistic locking would serialize all updates
  even when no actual conflict exists. Trade-off: the API layer must translate the
  conflict exception into a meaningful 409 and the caller must handle "refetch and
  retry."
- **Pessimistic locking (`SELECT ... FOR UPDATE`) reserved for narrow state-machine
  transitions** where a lost update means an invalid transition slips through silently —
  the cost of briefly serializing one row is lower than building retry logic for every
  caller of that specific transition. Trade-off: the lock must be held only across fast,
  local operations — never across a network call.
- **Virtual threads over a reactive rewrite** to scale blocking I/O (JPA, HTTP clients).
  Existing blocking code scales to far more concurrent requests per OS thread with zero
  rewrite — a full reactive migration would touch every layer for a benefit virtual
  threads already deliver at moderate traffic. Trade-off: virtual threads don't help
  CPU-bound work and can still be pinned by `synchronized` blocks around blocking calls —
  existing `synchronized` usage must be audited, not assumed safe.
- **A specific, human-readable 409 error code on lock conflict, never a generic 500.**
  A concurrent-edit conflict is expected and recoverable; a bare 500 tells the caller
  nothing actionable.
- **Bounded concurrency for expensive/external calls (the pattern `AIRequestQueue`
  already implements)**: a semaphore-gated queue plus a hard budget kill switch protects
  against both per-request contention and runaway cost, independent of per-user rate
  limiting. This is a third concurrency-control shape beyond optimistic/pessimistic
  locking — appropriate when the contended resource is an external, metered dependency
  (an LLM call) rather than a database row.

### Interview talking points
- **Q: When do you choose optimistic vs pessimistic locking?** Optimistic for
  low-contention entities where paying a lock cost on every write is wasteful;
  pessimistic for a narrow, fast state-transition check-and-write where losing the race
  means an invalid transition slips through, which is worse than briefly serializing
  that one row.
- **Q: What does `@Version` actually do at the SQL level?** Every UPDATE includes
  `WHERE id = ? AND version = ?`; zero affected rows because another transaction already
  bumped the version throws the optimistic-lock exception, mapped to a 409 rather than a
  raw 500.
- **Q: Why virtual threads instead of a reactive migration?** Existing blocking code
  handles far more concurrent requests without a rewrite touching every layer.
- **[TRAP] Q: With optimistic locking, does the loser's write get silently merged or
  lost?** Neither — the loser's UPDATE affects zero rows and throws immediately; nothing
  is silently merged or lost, but nothing is silently reconciled either. People assume
  "prevents lost updates" means the system merges the conflict for them; it just detects
  and rejects, and the caller decides what happens next.
- **Q: How does a semaphore-based request queue differ from row-level locking?** It
  bounds concurrency against an external, metered resource (like an LLM API) rather than
  guarding a database row — the goal is protecting a cost/rate budget, not preventing a
  lost update.

### Never do this
- Never hold a pessimistic lock across a network call — lock duration must be bounded by
  fast, local operations only.
- Never add `@Version` without also handling the resulting exception explicitly.
- Never assume virtual threads fix contention caused by `synchronized` blocks around
  blocking I/O — that still pins the carrier thread.
- Never apply pessimistic locking broadly "to be safe" — it serializes otherwise
  -independent requests.
- Never test a concurrency fix with a single manual run — race conditions need
  repeated/parallel execution to actually prove the fix.

### Definition of done
- [x] Bounded concurrency for external AI calls (`AIRequestQueue`, ADR-011)
- [ ] Optimistic locking + mapped 409 on any entity found to have real update contention
- [ ] Concurrent-update test proving exactly one success + one clean conflict response

---

## PHASE 5 — Caching / Perf

**Status: DONE — `SessionStore` abstraction (ADR-009), Redis-ready seam active**

### Scope (general pattern)
Cache-aside on read-heavy paths, event-driven eviction, and an N+1/indexing audit.

### Design decisions
- **Cache-aside, not write-through/read-through.** Cache-aside keeps the cache fully
  optional and disposable — the service works correctly cold or down, just slower; a
  write-through cache makes the cache a hard dependency for correctness. Trade-off: a
  stale-read window exists between a write and its eviction being processed, bounded by
  TTL.
- **Event-driven eviction over scattered manual `cache.evict()` calls.** Manual eviction
  calls are easy to forget when a new mutation path is added; driving eviction off the
  same domain event already produced for other consumers means eviction is automatic for
  every current and future producer of that event.
- **Short TTL plus event eviction, not TTL alone.** TTL-only caching means a stale read
  can persist for the full window; event-driven eviction handles the common case
  immediately, TTL is only the backstop for missed events — so it can stay short.
- **Composite cache keys including a query-parameter hash, not just an entity ID**, when
  caching filtered/paginated list results — otherwise two different filter/sort
  combinations collide on the same key.
- **In-memory cache over a read-replica database** as the first scaling lever. A read
  replica solves connection/IO-level read scaling but doesn't reduce
  serialization/query-planning cost per request the way a cache does, and it's heavier
  infrastructure than most traffic levels justify. Trade-off: cache correctness
  (staleness, eviction) becomes a new class of bug a read replica wouldn't introduce.

### Interview talking points
- **Q: Why cache-aside instead of write-through?** The service still returns correct
  (just slower) results with the cache cold or down entirely — write-through makes the
  cache a hard dependency for write correctness.
- **Q: How do you avoid serving stale data after a mutation?** A domain event already
  produced for other consumers also drives cache eviction — no separate "remember to
  bust the cache" code path. TTL is only the backstop.
- **[TRAP] Q: If a cache key for a filtered/paginated endpoint doesn't include the
  filter/sort params, what breaks?** Two different filtered requests collide on the same
  cache key and the second request silently gets the first request's cached (wrong)
  result set — invisible in a quick single-filter manual test, immediate under real
  multi-filter traffic. Fix: hash the full canonical query-param set into the key.

### Never do this
- Never cache a filtered/paginated list keyed only by page number.
- Never rely on scattered manual eviction calls as the only invalidation strategy —
  prefer event-driven eviction from one source of truth.
- Never add an index without confirming (via query-plan analysis) the optimizer
  actually uses it.
- Never let the service depend on the cache being up for correctness — cache failures
  must degrade to "slower," never to an error.
- Never set a long TTL as a substitute for real eviction.

### Definition of done
- [x] `SessionStore` abstraction implemented, Redis-ready seam in place (ADR-009)
- [x] Fixed-window rate limiting chosen over sliding window for command-budget cost
      reasons (ADR-010)
- [ ] Full cache-aside on hot read paths once a genuine hot path is identified beyond
      ephemeral session state

---

## PHASE 6 — Security

**Status: DONE — Google OAuth2 + JWT + RBAC**

### Scope (general pattern)
Authentication/authorization across the API surface: token validation, role-based
access control, ownership checks, and secrets out of config files.

### Design decisions
- **Re-validate the token at every service, not trust-forwarded-header-blindly** in any
  multi-service topology. Trusting a forwarded internal header assumes the internal
  network can never be reached directly — a fragile assumption once everything shares a
  cluster network. Trade-off: signature validation happens per hop instead of once — an
  acceptable CPU cost for the guarantee it buys.
- **RBAC via method-level annotations, not a centralized authorization service**, while
  the rule set stays simple enough to express declaratively. A separate authz service
  adds a network hop and a new single point of failure for a rule set that doesn't yet
  need it. Trade-off: authorization logic is spread across annotated methods rather than
  centralized — revisit if the rule set grows materially more complex.
- **Ownership checks (does this user own/relate to this resource) in the service layer**,
  not crammed into a declarative-annotation expression language. A framework annotation
  handles the role check cleanly; reaching into a nested request field to compare
  ownership becomes unreadable and hard to test in isolation.
- **Secrets via environment variables now, with the paid-secrets-manager path
  named-but-commented** — not a self-hosted secrets vault. A vault is real infrastructure
  with no traffic/compliance requirement forcing it yet; env vars satisfy "no hardcoded
  secrets" today, and the AWS-naming rule (Phase 11) documents the eventual production
  target without paying for it now.

### Interview talking points
- **Q: Why validate the JWT at every service instead of just at the edge/gateway?**
  Trusting a forwarded header assumes the internal network topology can never be reached
  directly — independent validation means no service's security depends on network
  topology.
- **Q: Why not build a separate centralized authorization service?** A simple role +
  ownership rule set is cheaper to express declaratively at each service than to route
  through a network hop that becomes its own single point of failure.
- **[TRAP] Q: If an endpoint has no explicit authorization annotation at all, is it
  protected or open?** By default, no method-level annotation means no restriction is
  enforced at that layer — open unless the global security filter chain itself requires
  authentication for all paths. The failure mode this is designed to catch is "I assumed
  something upstream handles it" — default-deny must be verified explicitly per
  endpoint, not assumed.

### Never do this
- Never trust a forwarded user-identity header without independently validating the
  token at each service that receives it.
- Never hardcode a signing key or credential in a config file "for local dev only."
- Never ship an endpoint with no explicit authorization rule and assume no one will call
  it wrong — default-deny, not default-allow.
- Never express an ownership/ABAC-style check purely in annotation expression syntax
  just to avoid service-layer code.
- Never log a raw token or password value, even at DEBUG level.

### Definition of done
- [x] Google OAuth2 + JWT validation active
- [x] RBAC enforced via method-level checks
- [x] Secrets via environment variables, not hardcoded
- [ ] Full per-endpoint authorization matrix reviewed for "no rule found" gaps
      (`docs/authz-matrix.md` exists — keep it current per new endpoint)

---

## PHASE 7 — Observability

**Status: PENDING — metrics wiring is the next concrete step**

### Scope (general pattern)
Structured logging, distributed tracing, and metrics/alerting across the service.

### Design decisions
- **A vendor-neutral instrumentation standard (e.g. OpenTelemetry), not a proprietary
  APM SDK.** The same instrumentation exports to a free local backend now and a paid
  backend later without re-instrumenting code — matches the "OSS active, paid deps
  commented" posture.
- **Correlation ID and trace ID kept as two related but distinct identifiers**, not
  unified into one. The correlation ID is simple and always available even if tracing
  instrumentation is partial or temporarily broken in one path; the trace ID carries
  full span-hierarchy semantics. Logging both together means neither is a single point
  of failure for debugging.
- **RED metrics (Rate, Errors, Duration) as the primary dashboard model**, not USE
  (Utilization, Saturation, Errors) as the starting point. RED answers "is the API
  healthy from a caller's perspective" directly; USE matters more once infrastructure
  capacity planning becomes the live question (Phase 8).
- **Audit logging as a separate stream from application logs.** Audit and operational
  logs have different retention, access-control, and query requirements; mixing them
  forces a bad compromise on one or the other.
- **Alert thresholds as committed config (alerting-as-code), not hand-clicked into a
  dashboard UI.** UI-configured alerts aren't versioned, aren't reviewable, and are
  silently lost on a redeploy.

### Interview talking points
- **Q: Why keep both a correlation ID and a trace ID instead of just one?** Log
  correlation keeps working even if tracing instrumentation is partial or broken in one
  path — neither signal is a single point of failure for debugging.
- **Q: Why RED metrics before USE metrics?** RED answers the caller-facing health
  question that matters most at moderate traffic; USE becomes primary once container/
  infra capacity questions are the live concern.
- **[TRAP] Q: If 100% of traces are sampled at real production traffic volume, what
  breaks?** Trace storage/export volume scales linearly with request volume and can
  overwhelm the tracing backend — invisible at learning-phase traffic, which is exactly
  why it isn't caught until a real incident. The fix is deliberately tuning the sampling
  rate down (or tail-based sampling) before any real-traffic deployment.

### Never do this
- Never add a high-cardinality field (user ID, email, free text) as a metric label — use
  it in logs/traces instead.
- Never configure an alert rule only in a dashboard UI with no corresponding committed
  config.
- Never mix audit-relevant events into general application logs to save a sink.
- Never carry a dev-environment 100%-sampling rate forward into a higher-traffic
  deployment without deliberately re-evaluating it.
- Never treat "a log line has a trace ID in it" as equivalent to "tracing works" —
  verify an actual multi-hop trace renders in the tracing UI.

### Definition of done (next up)
- [ ] Metrics wiring (Micrometer/Actuator or equivalent) for request rate/latency/error
      rate per endpoint
- [ ] Structured logging extended with trace/span IDs alongside the existing
      correlation ID
- [ ] At least one deliberately-triggered failure produces a correlated trace + log +
      metric spike, verified end-to-end

---

## PHASE 8 — DevOps

**Status: PARTIAL — Docker done locally; K8s manifests and CI/CD pending (hosting is
Railway/Vercel/Supabase, not self-managed K8s)**

### Scope (general pattern)
Containerize the service and stand up CI/CD with test-gated deploys.

### Design decisions
- **Multi-stage Docker builds, not a single fat image with build tooling baked into the
  runtime layer.** The final runtime image only needs the runtime + the built artifact —
  a smaller image means smaller attack surface, faster pulls, faster startup.
- **Free/local orchestration (e.g. Minikube) before a managed cloud cluster**, applying
  production-style naming/config even before the managed target is used, so the eventual
  move is a deploy-target change, not a rewrite. (For InterviewLab specifically: managed
  PaaS hosting — Railway/Vercel/Supabase — was chosen directly per the zero-paid-tools
  posture and free-tier availability, making a full K8s layer a Phase-8/11-style
  exercise to revisit only if traffic or portability demands it.)
- **The pipeline hard-fails the build on any test failure, not just on compile failure.**
  "No feature without tests" is meaningless if a broken test doesn't actually block a
  merge/deploy.
- **Environment/namespace isolation per branch type**, not one shared deploy target for
  every branch. A shared target means an in-progress, possibly-broken deploy can stomp
  on the environment others rely on to demo or test against.

### Interview talking points
- **Q: Why multi-stage Docker builds instead of one image with build tooling baked in?**
  The runtime image only needs the runtime + artifact — shipping build tooling bloats
  image size, slows startup, and widens attack surface for no runtime benefit.
- **Q: What's the difference between a liveness and a readiness probe, and why does it
  matter?** Liveness answers "is this process alive enough to keep running" — failure
  restarts the instance; readiness answers "is this instance ready for traffic right
  now" — failure just pulls it out of the load-balancing pool without restarting.
  Conflating them causes an orchestrator to kill instances that are just temporarily
  busy, not actually broken.
- **[TRAP] Q: If a container image builds and the container starts, does that mean the
  deployment is healthy?** No — a container can start and immediately crash-loop, or
  start but never pass its readiness check (e.g. can't reach its database), and the
  orchestrator reports the deployment unavailable despite a "successful" build and start.
  The actual health signal is the readiness/liveness probe status inside the runtime
  environment, not the build log.

### Never do this
- Never bake secrets into a Docker image layer — inject at runtime via the platform's
  secret/env mechanism.
- Never wire liveness and readiness checks to the identical check with identical
  thresholds — they protect against different failure modes.
- Never let a pipeline stage "soft fail" on test failures — a red test must hard-stop
  the pipeline before deploy.
- Never deploy a feature/experimental branch to the same environment production traffic
  depends on.
- Never assume a rollback command exists without having proven it actually recovers a
  bad deploy in this specific setup.

### Definition of done
- [x] Dockerized locally
- [ ] CI pipeline that hard-fails on test failure before any deploy step
- [ ] K8s manifests / equivalent portability layer only if a future portability or
      traffic requirement actually demands it (currently no trigger — PaaS hosting
      sufficient)

---

## PHASE 9 — AI Layer

**Status: PARTIAL — agents done (InterviewAgent, MentorAgent, AgentTools chain);
pgvector/RAG semantic search pending**

### Scope (general pattern)
Local-first LLM integration, embeddings + vector search, RAG, and tool-calling agents.

### Design decisions
- **Local/free model runtime (e.g. Ollama) over a paid hosted LLM API as the default.**
  Proves out the entire RAG/agent/tool-calling architecture at zero marginal cost per
  request; a proper provider abstraction (already InterviewLab's Strategy pattern —
  ADR-001, ADR-005) means swapping in a paid provider later is a config change, not a
  rewrite. Trade-off: local models are weaker than frontier hosted models — acceptable
  while proving the pipeline, not yet a customer-facing quality bar.
- **A Postgres vector extension (pgvector) over a dedicated vector database**, when data
  already lives relationally. One fewer piece of infrastructure to operate, and vector
  search lives alongside the relational data it's describing without a second database
  to keep in sync. Trade-off: less mature ANN indexing than a purpose-built vector DB at
  very large scale — revisit only if traffic actually demands it.
- **Semantic/vector search replaces naive exact-match filtering entirely**, rather than
  running both in parallel, once embeddings exist — a parallel exact-match system is
  redundant complexity once similarity search subsumes the same query and adds a
  strictly more valuable one ("find similar to X"). Trade-off: exact-match guarantees
  weaken with pure semantic search; a hybrid keyword+vector approach is the documented
  escape hatch if pure semantic search proves too fuzzy.
- **Tool-calling via the framework's structured function-calling abstraction**, not a
  hand-rolled prompt-parsing loop that asks the model to emit a specific string format.
  Hand-rolled parsing is fragile against model output drift; the framework abstraction
  is both less code and more robust.
- **An agent-exposed interface (e.g. MCP) is additive, not a replacement for the
  existing REST API.** REST remains the stable, versioned contract for direct API
  consumers; the agent interface is additional for AI-tool consumers specifically —
  collapsing them breaks contract-stability discipline. The agent's tool implementation
  must call the same service-layer method the REST controller calls — never a duplicate
  implementation.

### Interview talking points
- **Q: Why a local model runtime instead of a hosted LLM API for the default path?**
  Proves the full architecture at zero marginal cost; a proper `ChatModel`-style
  abstraction means swapping providers later is configuration, not architecture.
- **Q: Why pgvector instead of a dedicated vector database?** Vector search lives next
  to already-relational data without operating a second database service — a dedicated
  vector DB is real infrastructure most data volumes don't yet justify.
- **Q: How does an agent avoid duplicating business logic that already exists in a
  service class?** Every tool-annotated method calls the same service-layer method the
  REST controller calls — exactly one implementation regardless of which interface
  triggers it.
- **[TRAP] Q: If a user's free-text input contains "ignore all previous instructions and
  do X," what actually stops that from working?** Nothing about the model itself
  reliably stops it — that's why an explicit input-sanitization/guardrail layer must
  exist before user-supplied text ever reaches a prompt, and why an agent's tools are
  the only way state actually changes — a tool call to mutate data still goes through
  the same authorization-checked service method every other write path uses. The model's
  text output is never trusted to directly mutate data. People who've only chatted with
  a consumer LLM assume model-level "safety" covers this; in an agentic system with real
  tool access, the guardrail plus the unchanged authorization layer are what actually
  prevent it.

### Never do this
- Never let LLM-generated text directly trigger a state-changing action without going
  through the same authorization/business-rule layer every other write path uses.
- Never skip sanitizing user-supplied free text before it reaches a prompt — treat it as
  untrusted input, same as any other API boundary.
- Never let an agent tool reimplement logic that already exists in a service class.
- Never assume embedding similarity is equivalent to exact keyword matching — they
  answer different questions.
- Never forget to regenerate an embedding when its source content changes — a stale
  embedding silently degrades search relevance with no visible error.
- Never log full prompts/responses containing personal data to a general-purpose log
  stream without the same redaction discipline applied to passwords/tokens.

### Definition of done
- [x] Agent core: InterviewAgent + MentorAgent, AgentTools chain (Chain of
      Responsibility), Strategy-pattern AI provider swap (ADR-001, ADR-005)
- [ ] Embedding generation + pgvector storage for semantic search
- [ ] Guardrail/input-sanitization layer for user-supplied text reaching a prompt
- [ ] Prompt-injection test string demonstrably neutralized

---

## PHASE 10 — Frontend

**Status: PARKED — CDN React active (ADR-003); no build step by design**

### Scope (general pattern)
A typed frontend consuming the backend's API surface, with role-aware UI and a
deliberate state-management split between server state and local UI state.

### Design decisions
- **A dedicated server-state/data-fetching library (e.g. TanStack Query) for API data,
  local component state for UI-only state** — not a general-purpose global store for
  everything. Most of the state in this kind of app *is* server state with its own
  caching/invalidation needs; routing that through a general-purpose store reinvents the
  same manual cache-invalidation problem a caching layer already solves server-side.
- **Feature-folder structure, not a type-first split** (`components/`, `hooks/`,
  `services/` flattened globally). Keeps each domain's frontend code as cohesive as the
  backend's own domain boundaries.
- **Token stored in an httpOnly cookie, not `localStorage`.** `localStorage` is readable
  by any JavaScript on the page — directly exposed to XSS; an httpOnly cookie is
  invisible to JavaScript entirely. Trade-off: introduces CSRF as a concern
  `localStorage` didn't have — mitigated with a CSRF token or `SameSite` cookie
  attribute.
- **A typed API client generated from the backend's OpenAPI spec**, not hand-written
  fetch calls per endpoint — the frontend's understanding of the API can never silently
  drift from the actual backend contract.

### Interview talking points
- **Q: Why a server-state library instead of a general global store for everything?**
  Most state here is server data with caching/invalidation needs a data-fetching library
  solves directly — a general store just reinvents manual cache-invalidation the backend
  already solved.
- **Q: Why store the token in an httpOnly cookie instead of `localStorage`?**
  `localStorage` is readable by any JS on the page, directly exposed to XSS; an httpOnly
  cookie is invisible to JavaScript entirely.
- **[TRAP] Q: If the frontend hides a privileged action's button for a non-privileged
  role, is that hidden for security or for UX?** UX only — the actual security boundary
  is the server-side authorization check (Phase 6). Hiding the button prevents a
  confusing UI path; a direct API call with a crafted request still gets rejected
  server-side regardless of what the frontend renders. People who conflate "the button
  is visible" with "the action is permitted" overestimate a frontend-only check as a
  security control when it's a UX one.

### Never do this
- Never treat a frontend role/permission check as a security boundary — it's UX only;
  the backend is the only enforcement point.
- Never store an auth token in `localStorage` "for now."
- Never hand-write API response types that duplicate what the OpenAPI spec already
  defines.
- Never introduce a global client-state library to manage server data a caching
  data-fetching library already handles correctly.
- Never ship a form whose client-side validation is looser than the backend's actual
  rules.

### Definition of done (when unparked from CDN React)
- [ ] Generated types replace any hand-typed API response shapes
- [ ] httpOnly-cookie auth flow proven unreadable from browser JS
- [ ] A hidden-button backend action proven to still reject an unauthorized role

---

## PHASE 11 — AWS Concepts

**Status: PARTIAL — AWS-naming pattern applied in code/comments across swappable
backend domains; actual migration parked (see workspace CLAUDE.md AWS Naming Pattern)**

### Scope (general pattern)
Map every OSS component already running to its cloud-managed production equivalent —
conceptually, in naming, and via config — without incurring real cloud cost until
traffic demands it.

### Design decisions
- **Conceptual mapping and cloud-SDK-compatible code now; real spend deferred until
  traffic demands it.** Using a cloud-compatible SDK against a local/OSS stand-in (e.g.
  MinIO for S3) proves the code path works without paying for the managed service
  itself.
- **Evaluate each async/messaging flow individually for its cloud-managed target**, not
  a uniform "message queue X always maps to managed service Y." Some flows genuinely
  need ordering/replay guarantees and map to a Kafka-compatible managed service; simple
  fire-and-forget flows would run just as well, more cheaply, on a plain queue. A uniform
  mapping ignores a real cost/complexity optimization.
- **Cloud-named classes stay backed by the OSS implementation even at this phase** — the
  naming pattern was designed precisely so the swap to the real managed service is a
  configuration change (endpoint, credentials) at the point traffic actually demands it,
  not a rewrite pulled forward before it's needed.
- **Cost estimation is a documented exercise tied to a stated hypothetical traffic
  number**, not a vague "it would cost more" statement — a specific, defensible estimate
  is what an interview or a real planning exercise actually needs.

### Interview talking points
- **Q: Why keep everything on OSS/local stand-ins instead of using the real managed
  service from the start?** Every service was built against a cloud-API-compatible OSS
  stand-in with cloud-style naming from day one specifically so a later migration is
  verifying a config-only swap, not writing new integration code under migration
  pressure.
- **Q: Is a 1:1 tool mapping always the right call (e.g. "message queue always maps to
  the managed Kafka-equivalent")?** No — some flows need the managed Kafka-equivalent's
  ordering/replay guarantees; simple fire-and-forget flows run just as well, more
  cheaply, on a plain managed queue. Evaluate per flow, not as a blanket rule.
- **[TRAP] Q: If a config swap works flawlessly against a local OSS-compatible stand-in
  in every test, does that guarantee it works identically against the real managed
  service?** Not necessarily — "compatible API" covers the core surface (put/get/list,
  etc.) but the real managed service has behaviors (consistency edge cases, IAM-based
  access policies, region-specific latency, rate limits) a local stand-in doesn't
  reproduce. The only way to know is to actually test against the real service at least
  once before calling the swap "proven."

### Never do this
- Never provision real paid cloud infrastructure beyond free-tier verification testing
  until traffic actually demands it.
- Never apply a uniform "OSS tool X always maps to managed service Y" rule without
  checking whether the specific usage needs that service's distinguishing guarantees.
- Never claim a "config-only" swap is proven without actually testing it against the
  real cloud-compatible endpoint at least once.
- Never retroactively edit earlier infrastructure decisions to look "cloud-ready from
  the start" — add new, separately-reasoned changes instead, preserving original
  decision history.
- Never publish a cost estimate with no stated traffic assumption behind it.

### Definition of done
- [x] AWS-naming pattern applied across swappable backend domains (code + commented
      pom.xml/yml entries)
- [ ] Actual migration/config-only-swap verification against a real managed service
      (parked — see workspace CLAUDE.md P3 AWS migration trigger: traffic demand)
- [ ] Per-component cost estimate tied to a stated hypothetical traffic number

---

## LIVING DOCUMENT RULES

- Never regenerate this file wholesale — patch sections in place.
- When a phase's status changes (PARKED → SEAM READY → PARTIAL → DONE), update its
  status line and check off the relevant Definition of Done items as they're actually
  verified, not all at once.
- Append new decisions as they emerge during implementation — never overwrite existing
  "why chosen / trade-off" reasoning, even if a later decision supersedes it.
- Cross-reference `docs/decisions/ADR-*.md` for InterviewLab's actual implemented
  decisions where a phase above references one directly.
