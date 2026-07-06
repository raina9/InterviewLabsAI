# ADR-009: SessionStore abstraction for ephemeral state

## Status
Accepted

## Context
QuizService, DrillService, and CodeChallengeService each held their in-flight session state (quiz progress, drill history, generated challenges) in a private `ConcurrentHashMap<UUID, ...>`. That works for a single JVM instance but breaks the moment InterviewLab runs more than one instance (a restart loses every in-progress quiz/drill/challenge, and two instances behind a load balancer don't share state at all). Rate limiting had the same shape of problem waiting to happen: a per-user request counter needs the same kind of shared, TTL-bound storage. Introducing Redis directly into each of the four call sites would have meant four bespoke integrations and four places to get the TTL-and-serialization details wrong.

## Decision
A single `SessionStore` interface (`put(key, value, ttlHours)`, `get(key, type)`, `delete(key)`, `increment(key, ttlHours)`) sits behind the existing Swappable Backend Pattern:
- `InMemorySessionStore` — `ConcurrentHashMap` + lazy TTL check on read. Active by default (`SESSION_STORE=memory` or unset), zero infra.
- `RedisSessionStore` — active only when `SESSION_STORE=redis`. Uses `GenericJackson2JsonRedisSerializer` exclusively (wired in `RedisConfig`), never JDK serialization.
- `QuizService`, `DrillService`, `CodeChallengeService`, `RateLimitService` all inject `SessionStore`, never a concrete implementation.
- `QuizSessionState` and `DrillSessionState` (plain mutable classes, not records) needed an explicit `@JsonCreator` constructor added — Jackson can deserialize the domain records (`CodeChallenge`, `QuizQuestion`) natively from their `RecordComponent` metadata, but a hand-written multi-field constructor needs an annotated creator to round-trip through Redis reliably.
- Every service now re-`put()`s the session object after mutating it in place (e.g. `state.advance(correct)` followed by `sessionStore.put(...)`). This was a no-op under the old `ConcurrentHashMap` (same object reference stayed in the map), but is required correctness once Redis deserializes a fresh object on every `get()` — skipping the re-`put()` would silently drop every state mutation under the Redis backend.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Redis client wired directly into each service | Rejected | Four bespoke integrations, four places to get TTL/serialization wrong, no free/paid seam |
| `@Cacheable`/Spring Cache abstraction | Rejected | Built for read-through caching of pure functions, not a fit for mutable session state with explicit TTL and atomic increment semantics |
| **`SessionStore` interface, `@ConditionalOnProperty`-gated** | **Chosen** | Same mechanism as every other swappable backend in this codebase (ADR-004) — one seam, one env var, zero code change to swap |

## Consequences
- `SESSION_STORE=memory` (default) requires zero infrastructure and is what every test in the suite runs against — `InMemorySessionStore` is constructed directly in tests, no Redis needed anywhere in CI.
- `DeploymentModeValidator` refuses to start in `DEPLOYMENT_MODE=production` without `REDIS_URL` configured, because the in-memory store silently loses all state across a restart or a second instance — that failure mode needed to be a loud startup error, not a quiet data-loss bug discovered in production.
- The `@JsonCreator` constructors added to `QuizSessionState`/`DrillSessionState` are dead weight under the memory backend (never invoked) but are load-bearing the moment Redis activates — worth the small verbosity cost given they'd otherwise fail silently on first real deploy.
- New domains needing ephemeral shared state (future) reuse `SessionStore` rather than reaching for a Redis client directly.

## Lesson
Migrating from an in-memory map to a distributed store is not just a storage swap — it changes the mutation model from "shared object reference" to "value copied out, mutated, and explicitly written back." Every call site that mutated the fetched object in place had to be audited for a missing re-`put()`, because the bug that missing call produces (state mutations silently not persisting) only reproduces once the paid/Redis path is actually exercised — it is invisible under the default in-memory backend and under every test in the suite.

## Interview Talking Point
"When I moved quiz/drill/code-challenge session state from a `ConcurrentHashMap` to a swappable `SessionStore` abstraction backed by Redis, the interesting bug wasn't the interface design — it was that in-place mutation of a fetched object works by accident under a `ConcurrentHashMap` (you're holding the same reference) but silently drops every write under Redis, where `get()` deserializes a brand-new object each call. I had to audit every service method that mutated session state and add an explicit re-`put()` after each mutation, and add Jackson creator annotations to the hand-written state classes so they'd actually round-trip through `GenericJackson2JsonRedisSerializer`."

See also: [[ADR-004-conditional-on-property-pattern]], [[ADR-010-fixed-window-ratelimit]]
