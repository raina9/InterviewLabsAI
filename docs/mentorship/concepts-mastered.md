---
name: concepts-mastered
---

# Concepts Mastered

Format: Concept | Implemented | Can explain | Interview ready

"Implemented" = shipped in InterviewLab's codebase. "Can explain" = can walk through the why, not just the what. "Interview ready" = can defend the trade-offs under follow-up questioning. See [[weak-areas]] for the inverse of this list — concepts *not* covered here.

## Java 25
| Concept | Implemented | Can explain | Interview ready |
|---|---|---|---|
| Virtual threads | Yes — default request handling (Spring Boot 4 default on Java 25) | Yes | Yes |
| Records | Yes — DTOs, value objects throughout (`AIOptions`, `AgentContext`, `MentorFeedback`, `InterviewTurnResult`) | Yes | Yes |
| Sealed interfaces | Yes, then deliberately un-sealed — [[../decisions/ADR-005-sealed-interface-removed]] | Yes (including *why it was removed*) | Yes |
| Pattern matching / exhaustive enum switch | Yes — `AIProviderFactory.getProvider()` | Yes | Yes |

## Spring
| Concept | Implemented | Can explain | Interview ready |
|---|---|---|---|
| `@ConditionalOnProperty` swappable backends | Yes — AI provider, messaging, storage, orchestration — [[../decisions/ADR-004-conditional-on-property-pattern]] | Yes | Yes |
| Spring AI 2.0 (Ollama integration) | Yes — [[../decisions/ADR-001-ollama-over-gemini]], [[../decisions/ADR-006-spring-ai-property-path]] | Yes | Yes |
| Flyway migrations | Yes — versioned schema, no auto-ddl in prod | Yes | Yes |
| Spring Security (dual-mode: dev token / OAuth2 + JWT) | Yes — `SecurityConfig`, `DevTokenFilter`, `JwtAuthFilter`, `OAuth2SuccessHandler` | Yes | Partial — production OAuth setup itself is parked (see `authz-matrix.md` gaps) |

## AI Engineering
| Concept | Implemented | Can explain | Interview ready |
|---|---|---|---|
| Strategy pattern for provider abstraction | Yes — `AiProviderStrategy` + `AIProviderFactory` | Yes | Yes |
| Chain of Responsibility for context assembly | Yes — `AgentToolChain`, 5 `AgentTool` beans, `@Order`-sequenced | Yes | Yes |
| Sliding-window context management | Yes — see `ai-architecture.md` context management section | Yes | Partial — implemented pragmatically, not benchmarked against alternatives |

## Patterns
| Concept | Implemented | Can explain | Interview ready |
|---|---|---|---|
| Swappable backend pattern | Yes — 4 domains — [[../decisions/ADR-004-conditional-on-property-pattern]] | Yes | Yes |
| ADR (Architecture Decision Record) discipline | Yes — 8 ADRs in `docs/decisions/` | Yes | Yes |
| 9-section spec discipline | Yes — used for feature planning across checkpoints | Yes | Partial — can describe the practice, hasn't been asked to defend it as a named methodology in an interview yet |

See also: [[weak-areas]], [[../mastery-tracker]]
