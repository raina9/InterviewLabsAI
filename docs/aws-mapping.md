# AWS Mapping

Every active OSS backend in this codebase has a documented AWS-equivalent swap path, following
the workspace `AWS NAMING PATTERN` and `SWAPPABLE BACKEND PATTERN` standards (CLAUDE.md). None
of these are active — this table formalizes the swap paths already commented in `pom.xml` and
`application.yml` so the migration path is legible in one place without reading dependency
comments. Unpark trigger for all rows: traffic demand exceeds the zero-cost tier (CLAUDE.md
`GOLDEN RULES` — "Zero paid tools until traffic demands").

## Mapping Table

| Domain | OSS Active | AWS Equivalent | Switch Mechanism | Code Change Needed |
|---|---|---|---|---|
| Database | PostgreSQL (Supabase prod / Docker local) | RDS for PostgreSQL | `DB_URL` → RDS endpoint (`spring.datasource.url`, `application.yml:31`) | None — `org.postgresql:postgresql` driver speaks RDS PostgreSQL natively. IAM auth (instead of password auth) would add `software.amazon.awssdk:rds` for token generation only. |
| Storage | `LocalFileStorageService` (filesystem, `storage/` package) | S3 (`S3StorageService`) | `STORAGE_MODE=s3` (`app.storage.mode`, `application.yml:387`, default `local`) | Implement `S3StorageService` against the existing `StorageService` interface, annotate `@Service @ConditionalOnProperty(name="app.storage.mode", havingValue="s3")`, uncomment `software.amazon.awssdk:s3` + AWS SDK BOM in `pom.xml`. MinIO is the local/OSS S3-compatible stand-in today (same SDK surface, different endpoint). |
| Session store (ephemeral quiz/drill/code state) | `InMemorySessionStore` (default) / `RedisSessionStore` (opt-in, self-hosted or Upstash) | ElastiCache for Redis | `SESSION_STORE=redis` + `REDIS_URL` → ElastiCache primary endpoint (`app.session.store`, `application.yml:354`, default `memory`) | None — `spring-boot-starter-data-redis` (Lettuce) already active and speaks the Redis protocol ElastiCache exposes. Use `rediss://` for in-transit encryption if enabled on the cluster. |
| Messaging | `SyncEventPublisher` (in-process, default) | SQS (or MSK for a Kafka-compatible managed broker) | `MESSAGING_MODE=kafka` today unparks `KafkaEventPublisher` (self-hosted/Upstash Kafka); an SQS path would be a new `SQSMessagePublisher` (`app.messaging.mode`, `application.yml:377`, default `sync`) | Implement `SQSMessagePublisher` against `EventPublisher` (`event/EventPublisher.java`), annotate `@Component @ConditionalOnProperty(name="app.messaging.mode", havingValue="sqs")`, uncomment `software.amazon.awssdk:sqs` + AWS SDK BOM. MSK is the closer swap if Kafka semantics (partitioning, consumer groups) are load-bearing rather than SQS's simpler queue model. |
| AI inference | Ollama (local `llama3.2`, zero cost) | Bedrock (model-agnostic managed inference) | `AI_PROVIDER=bedrock` would require a new `BedrockProvider` implementing `AiProviderStrategy` (`app.ai.provider`, `application.yml:242`, default `ollama`) | Not scaffolded today — unlike storage/messaging/session-store, no commented Bedrock dependency or provider class exists yet. Closest existing precedent: `GeminiProvider` (direct `RestClient` call, no SDK dependency) — a `BedrockProvider` would likely follow the AWS SDK v2 Bedrock Runtime client instead. `ClaudeProvider`/`OpenAIProvider` are the current parked non-Ollama providers; Bedrock is not one of them. |
| Logs | Loki + Logback appender | CloudWatch Logs | New `CloudWatchLogger` component, no env var switch scaffolded | Not scaffolded today. `software.amazon.awssdk:cloudwatchlogs` is commented in `pom.xml` with activation steps noted but no interface/impl pair exists yet — unlike the four domains above, this one hasn't had the Swappable Backend Pattern applied (no seam interface). |
| Metrics | Prometheus (Micrometer registry, `/actuator/prometheus`) | Amazon Managed Service for Prometheus (AMP) | Scrape target reconfiguration only | None — AMP ingests the same Prometheus remote-write protocol; `micrometer-registry-prometheus` output format is unchanged. Not a code-level swap, an infra-level one. |
| APM tracing | None active (OpenTelemetry collector not wired end-to-end — see [[mastery-tracker]]) | AWS X-Ray, or Datadog APM (already parked, see `pom.xml` Datadog comment) | N/A | Out of scope for this table — no OSS-active tracing backend exists to map from yet. |

## Why Some Rows Have No Code Scaffolding Yet

Storage, session store, and messaging follow the full Swappable Backend Pattern (interface +
`@ConditionalOnProperty` free impl + documented paid impl activation steps) — see
[[lld/swappable-backend-pattern]]. AI inference (Bedrock) and logs (CloudWatch) do not yet have
that scaffolding: `pom.xml` documents the AWS dependency and activation steps as a comment, but
no interface implementation exists to activate. This mirrors the current parked-item state in
CLAUDE.md `PARKED ITEMS` (P3 — "AWS migration — Free tier sufficient — Trigger: Traffic demand")
and `ADR-004` (`@ConditionalOnProperty` on every swappable backend) — the pattern is applied
per-domain as each domain's paid alternative becomes worth scaffolding, not all at once
speculatively.

See also: [[decisions/ADR-004-conditional-on-property-pattern]], [[lld/swappable-backend-pattern]]
