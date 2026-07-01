# ADR-004: @ConditionalOnProperty for every swappable backend

## Status
Accepted

## Context
InterviewLab has multiple external dependencies that need a free/OSS implementation active by default and a paid/cloud implementation available but dormant: AI provider (Ollama vs Gemini/Claude/OpenAI), messaging (in-process `SyncEventPublisher` vs Kafka), storage (`LocalFileStorageService` vs `S3StorageService`), and agent orchestration (`LocalAgentOrchestrator` vs a future cloud orchestrator). Zero-cost-until-traffic-demands is a Golden Rule, not a one-off decision — it needed a repeatable mechanism, not a bespoke solution per domain.

## Decision
Every swappable backend follows the same contract:
1. An interface defines the contract (`EventPublisher`, `StorageService`, `AgentOrchestrator`, `AiProviderStrategy`)
2. The free implementation is annotated `@ConditionalOnProperty(name = "app.<domain>.mode", havingValue = "<free-value>", matchIfMissing = true)` — active by default with zero configuration
3. The paid implementation carries the *same* conditional, gated on its own value — it never instantiates unless explicitly configured
4. A single env var (`MESSAGING_MODE`, `STORAGE_MODE`, `AGENT_ORCHESTRATION_MODE`) controls the switch
5. Callers inject the interface, never the concrete class — the bean swap is invisible to consumers

Where a bean can't cleanly be gated this way (AI providers, which are selected per-request rather than one-active-bean), the equivalent is done via `Optional<T>` injection in `AIProviderFactory` — `ClaudeProvider`/`OpenAIProvider` are `@ConditionalOnProperty`-gated and injected as `Optional`, so the factory never fails to start when a parked provider has no credentials configured.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Spring Profiles (`@Profile("prod")`) | Rejected | Binary and coarse — couples the switch to the whole application's profile rather than to one domain's config, and doesn't compose well when multiple domains need independent switches |
| **`@ConditionalOnProperty` per domain** | **Chosen** | Fine-grained, one domain switches independently of the others, matches Spring Boot's own internal pattern |

## Consequences
- Unused/unconfigured paid providers never instantiate — no wasted bean creation, no accidental credential requirement at startup for a provider nobody's using
- Switching free → paid is a single env var change, zero code change, per the pattern documented in every affected `package-info.java`
- Paid implementation code lives in the repo permanently (even before it's built, the `package-info.java` documents the exact activation steps) — nothing has to be reverse-engineered when the unpark trigger fires
- Slight verbosity cost: every swappable domain needs its own `package-info.java` documenting both implementations and the switch mechanism, and its own conditional-property test coverage to prove the default doesn't accidentally activate the paid path

## Lesson
This isn't a bespoke pattern — it's the same mechanism Spring Boot's own auto-configuration uses internally (e.g. `DataSource` auto-configuration activates based on classpath presence and properties). Recognizing and reusing Spring's own idiom instead of inventing a custom factory/registry saved significant design time and produces code any Spring engineer immediately recognizes.

## Interview Talking Point
"Every external dependency in the system — AI provider, messaging, storage, agent orchestration — follows the same contract: interface plus a `@ConditionalOnProperty`-gated free implementation and a `@ConditionalOnProperty`-gated paid implementation, switched by a single env var. It's not something I invented; it's literally how Spring Boot's own `DataSource` auto-configuration works, applied consistently across every swappable domain in the app."

See also: [[ADR-001-ollama-over-gemini]], [[ADR-002-monolith-first]]
