# ADR-002: Monolith-first architecture

## Status
Accepted

## Context
Target deployment for V1 is free-tier hosting (Railway backend + Vercel frontend + Supabase database — see CLAUDE.md `STACK`). Free-tier compute and networking budgets cannot absorb the operational overhead a microservice split would add: multiple deployable units, inter-service network calls, service discovery, distributed tracing across process boundaries, and N× the cold-start cost on a platform that already sleeps idle instances.

InterviewLab has 15+ domain packages (`agent`, `ai`, `assessment`, `auth`, `code`, `curriculum`, `drill`, `event`, `feedback`, `interview`, `proficiency`, `profile`, `psychology`, `quiz`, `session`, `storage`, `voice`) — a natural microservice-per-domain instinct exists, but nothing about V1's traffic (solo-founder, pre-launch) justifies the distributed-systems tax.

## Decision
Single Spring Boot monolith (`interview-lab` artifact), with **clean package boundaries by domain** rather than by technical layer. Each domain package (`agent`, `session`, `interview`, `proficiency`, etc.) owns its own controller, service, repository, and DTOs — package-private where possible — so any package is extractable into its own service later without a redesign, only a deployment change.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Microservices per domain | Rejected | Premature — no traffic, no team, no scaling pressure to justify the operational cost |
| **Monolith, domain-package boundaries** | **Chosen** | Simple single-JAR deploy, boundaries already shaped for future extraction |

## Consequences
- Deploy is a single JAR (Spring Boot layered build via `spring-boot-maven-plugin`, `layers.enabled=true` for Docker cache efficiency) — one Railway service, one health check, one log stream
- Harder to scale any single domain horizontally in isolation later (e.g. scaling `code` challenge execution independent of `interview` traffic) — accepted trade for V1
- The Swappable Backend Pattern ([[ADR-004-conditional-on-property-pattern]]) already isolates the domains most likely to need independent scaling behind interfaces (`EventPublisher`, `StorageService`, `AgentOrchestrator`) — so the extraction seam exists ahead of the need, without paying the distributed-systems cost today
- `AGENT_ORCHESTRATION_MODE=cloud` (parked, `CloudAgentOrchestrator`) is the designed unpark path if agent execution specifically needs to move off the monolith before the rest of the system does

## Lesson
Premature distribution is the root of many evils. A monolith with clean, extractable package boundaries defers the *cost* of microservices without losing the *option* — the boundaries are drawn by domain now, so extraction later is a deployment change, not an architecture change.

## Interview Talking Point
"I went monolith-first with domain-package boundaries, the same pattern Netflix and Amazon both started with before scale forced extraction. The boundaries already match where I'd cut services later — `EventPublisher`, `StorageService`, and `AgentOrchestrator` are already behind interfaces with a `@ConditionalOnProperty` seam, so extracting any one of them into its own service is a deploy-time decision, not a rewrite."

See also: [[ADR-004-conditional-on-property-pattern]]
