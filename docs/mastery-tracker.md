# Mastery Tracker

Full concept mastery table across the four domains most relevant to the target trajectory (CLAUDE.md `WHO`: Java Backend Engineer → Senior → Staff → Architect → AI Architect). Each row: Implemented in this codebase | Can explain the why | Interview ready under follow-up | Risk level if asked cold.

Risk level key: **LOW** (solid, demonstrated, defensible) · **MEDIUM** (understood, thin hands-on) · **HIGH** (gap — see [[mentorship/weak-areas]] for detail).

## Java 25 Features

| Concept | Implemented | Can Explain | Interview Ready | Risk |
|---|---|---|---|---|
| Virtual threads | Yes — Spring Boot 4 default request handling on Java 25 | Yes | Yes | LOW |
| Records (immutable DTOs/value objects) | Yes — `AIOptions`, `AgentContext`, `MentorFeedback`, `InterviewTurnResult`, all `AiProperties` nested config | Yes | Yes | LOW |
| Sealed interfaces | Implemented then deliberately removed — [[decisions/ADR-005-sealed-interface-removed]] | Yes, including the removal reasoning | Yes | LOW |
| Pattern matching / exhaustive enum switch | Yes — `AIProviderFactory.getProvider()` | Yes | Yes | LOW |
| Java 25 LTS discipline (no non-LTS anywhere) | Yes — enforced project-wide, `pom.xml` `<java.version>25</java.version>` | Yes | Yes | LOW |

## Spring Boot

| Concept | Implemented | Can Explain | Interview Ready | Risk |
|---|---|---|---|---|
| `@ConditionalOnProperty` swappable backends | Yes — 4 domains — [[decisions/ADR-004-conditional-on-property-pattern]] | Yes | Yes | LOW |
| Spring Security dual-mode auth (dev token / OAuth2 + JWT) | Yes — `SecurityConfig`, `DevTokenFilter`, `JwtAuthFilter` | Yes | Yes | LOW |
| Flyway versioned migrations | Yes — no auto-ddl anywhere | Yes | Yes | LOW |
| Spring AI 2.0 integration (Ollama) | Yes — [[decisions/ADR-001-ollama-over-gemini]], [[decisions/ADR-006-spring-ai-property-path]] | Yes | Yes | LOW |
| `@ConfigurationProperties` externalized config discipline | Yes — `AiProperties`, `AgentProperties`, `AuthProperties`, `JwtProperties` — zero hardcoded values | Yes | Yes | LOW |
| GlobalExceptionHandler / structured error responses | Yes — `ApiError`, `ErrorCode` enums per domain, standard structure across all controllers | Yes | Yes | LOW |
| Role-based authorization (`@PreAuthorize`, `hasRole`) | Partial — `Role` enum + in-controller checks (`AdminController`, `SystemFeedbackController`), no `@EnableMethodSecurity`/`@PreAuthorize` — [[authz-matrix]] | Yes, including the trade-off for not using method security at 2 check points | Yes | MEDIUM |
| Spring Kafka (real consumer/producer, not just config) | Seam only — `SyncEventPublisher` is the only running implementation | Yes, can describe intended design | No — never run a live consumer | HIGH |
| Spring Boot filter auto-registration vs security chain | Yes — found and fixed live: `@Bean`-declared `jwtAuthFilter()`/`devTokenFilter()` were double-registered (servlet container + security chain) — [[mentorship/session-learnings]] "The Masked Bug Chain" | Yes | Yes | LOW |
| `@WebMvcTest` slices + `@ConfigurationProperties` binding | Yes — `@EnableConfigurationProperties` on a slice-imported `@Configuration` class vs relying on `@ConfigurationPropertiesScan` — `docs/lld/auth-flow.md` | Yes | Yes | LOW |

## Distributed Systems

| Concept | Implemented | Can Explain | Interview Ready | Risk |
|---|---|---|---|---|
| Eventual consistency / event-driven design (conceptual) | Partial — `EventPublisher` abstraction models the seam, `SyncEventPublisher` is synchronous (no actual eventual consistency yet) | Yes | Partial | MEDIUM |
| Message ordering, duplicate events, idempotent consumers | Config present (`enable.idempotence: true`, `isolation.level: read_committed`) but never exercised against a live broker | Yes, from config/docs | No | HIGH |
| Kafka delivery guarantees, consumer lag, DLQ, retry topics, schema evolution | Not implemented | Partial — can describe concepts | No | HIGH |
| Distributed transactions (2PC, Saga) | Not implemented — single Postgres instance, no cross-service transaction has ever been needed | Partial — conceptual only | No | HIGH |
| Service discovery | Not applicable — monolith, [[decisions/ADR-002-monolith-first]] | Yes — can explain why it's not needed *yet* and what would change it | Yes | LOW |
| Cursor-based / keyset pagination | Not implemented — `Pageable` offset pagination only | Partial — can describe the trade-offs | No | HIGH |
| Circuit breaker / bulkhead / fallback (Resilience4j) | Not implemented — only request-level timeout exists | Partial | No | HIGH |
| Distributed tracing (correlation ID, trace ID) | Partial — structured logging with correlation ID is a stated non-negotiable, no OpenTelemetry collector wired end-to-end | Yes, conceptually | Partial | MEDIUM |

## AI Engineering

| Concept | Implemented | Can Explain | Interview Ready | Risk |
|---|---|---|---|---|
| Strategy pattern for LLM provider abstraction | Yes — [[ai-architecture]] | Yes | Yes | LOW |
| Chain of Responsibility for context assembly | Yes — 5-tool `AgentToolChain` | Yes | Yes | LOW |
| Multi-agent boundary design (InterviewAgent / MentorAgent separation) | Yes — enforced at the class level, no cross-agent calls | Yes | Yes | LOW |
| Sliding-window context management | Yes — `SessionHistoryTool` windowed history | Yes | Partial — not benchmarked against alternatives | MEDIUM |
| Token budget management per call site | Yes — `AiProperties` per-domain temperature/maxTokens | Yes | Yes | LOW |
| Prompt injection defense / output guardrails | **No** — documented gap, see [[ai-architecture]] | Yes — can describe what's missing and why | No | HIGH |
| RAG (retrieval-augmented generation) | Not implemented — `QnAReferenceTool` is a stub | Partial — conceptual only | No | HIGH |
| Vector DB / embeddings (pgvector, `nomic-embed-text`) | Not implemented | Partial — conceptual only | No | HIGH |
| Agentic evaluation / AI observability | Not implemented — no eval harness, no AI-specific tracing/metrics beyond app logs | Partial | No | MEDIUM |

See also: [[mentorship/concepts-mastered]], [[mentorship/weak-areas]], [[ai-architecture]]
