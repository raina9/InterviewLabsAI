# Swappable Backend Pattern — LLD

Source-cited low-level design of the repeatable free/paid backend-swap mechanism used
across every domain in CLAUDE.md's `SWAPPABLE BACKEND PATTERN` table. This is the same
mechanism Spring Boot's own auto-configuration uses internally (e.g. `DataSource`
auto-config activates based on classpath/properties) — applied deliberately here rather
than invented from scratch.

## Plain English

For every backend concern that has a free/local option and a paid/cloud option
(storage, session state, messaging, agent orchestration), the codebase defines one
interface and two implementations. Exactly one implementation is active at a time,
decided entirely by a single environment variable read at startup — never by an `if`
statement scattered through business logic. Switching from free to paid means setting
one env var and, for the two domains where the paid side isn't built yet, writing one
new class against the existing interface. No caller code changes either way.

## The Contract (6 steps, from CLAUDE.md, verified against code)

1. **Interface** — contract only, no implementation detail
2. **Free implementation** — `@ConditionalOnProperty(name="app.<domain>.mode", havingValue="<free-value>", matchIfMissing=true)`
3. **Paid implementation** — `@ConditionalOnProperty(name="app.<domain>.mode", havingValue="<paid-value>")`, code always present in the repo, guarded so it never instantiates unconfigured
4. **`pom.xml`** — paid dependency commented, with what it replaces / why commented / exact uncomment+config steps
5. **`package-info.java`** — documents both implementations and the switch mechanism
6. **Single env var** — zero code change to flip

## Worked Example — `SessionStore`

```java
// 1. Interface — session/SessionStore.java
public interface SessionStore {
    <T> void put(String key, T value, long ttlHours);
    <T> T get(String key, Class<T> type);
    void remove(String key);
    long increment(String key, long ttlHours);
}

// 2. Free — InMemorySessionStore.java
@Component
@ConditionalOnProperty(name = "app.session.store", havingValue = "memory", matchIfMissing = true)
public class InMemorySessionStore implements SessionStore { /* ConcurrentHashMap + lazy TTL */ }

// 3. Paid — RedisSessionStore.java
@Component
@ConditionalOnProperty(name = "app.session.store", havingValue = "redis")
public class RedisSessionStore implements SessionStore { /* RedisTemplate, GenericJackson2JsonRedisSerializer */ }
```

Switch: `SESSION_STORE=redis` + `REDIS_URL`. Consumers (`QuizService`, `DrillService`,
`CodeChallengeService`, rate limiting) inject `SessionStore`, never a concrete class —
the swap is invisible to them. `DeploymentModeValidator` additionally **enforces** the
switch at startup: `DEPLOYMENT_MODE=production` without `REDIS_URL` fails fast with
`IllegalStateException` rather than silently running the single-instance in-memory store
in a multi-instance deploy (`config/DeploymentModeValidator.java:47-52`).

## Applied Domains

| Domain | Interface | Free (default) | Paid | Switch env var | Paid impl exists? |
|---|---|---|---|---|---|
| Storage | `StorageService` | `LocalFileStorageService` | `S3StorageService` | `STORAGE_MODE` | **No** — interface + free impl only, S3 not scaffolded |
| Session store | `SessionStore` | `InMemorySessionStore` | `RedisSessionStore` | `SESSION_STORE` | **Yes** — both implementations present |
| Messaging | `EventPublisher` | `SyncEventPublisher` | `KafkaEventPublisher` | `MESSAGING_MODE` | Parked per CLAUDE.md `PARKED ITEMS` (P1 — trigger: multi-instance or async) |
| Agent orchestration | `AgentOrchestrator` | `LocalAgentOrchestrator` | `CloudAgentOrchestrator` | `AGENT_ORCHESTRATION_MODE` | **No** — seam only, see [[agent-architecture]] |
| AI provider | `AiProviderStrategy` | `OllamaProvider` | `GeminiProvider`/`ClaudeProvider`/`OpenAIProvider` | `AI_PROVIDER` | **Yes** — all four wired (Gemini active-but-parked as a full alternative, not just scaffolding) — see [[ai-provider-abstraction]] |

Full AWS-equivalent mapping (RDS, S3, ElastiCache, SQS/MSK, Bedrock) per domain:
[[aws-mapping]].

## Why Two Domains Have No Paid Implementation Yet

`StorageService`/`S3StorageService` and `AgentOrchestrator`/`CloudAgentOrchestrator`
follow steps 1-2 only — the interface and free implementation exist and are in active
use, but no paid implementation has been written. This is consistent with the pattern's
own philosophy: the interface seam is built *before* it's needed so the free
implementation is never a dead end, but the paid implementation itself is only written
when its unpark trigger fires (CLAUDE.md `PARKED ITEMS` — "traffic demand", "explicit
owner instruction"), not speculatively. `pom.xml` still documents the exact dependency
and activation steps as a comment for both, so the swap-in cost when triggered is
"write one class," not "research the dependency from scratch."

## Common Mistake This Pattern Prevents

Without `@ConditionalOnProperty` on **both** implementations, Spring would either fail
to start (two beans satisfying one `@Autowired` interface dependency, ambiguous) or
silently wire whichever bean happened to be scanned last. Guarding both sides — free
with `matchIfMissing=true`, paid without — makes exactly one bean eligible for any given
config value, and makes "no config set" resolve deterministically to the free option.

See also: [[aws-mapping]], [[ai-provider-abstraction]], [[agent-architecture]],
[[decisions/ADR-004-conditional-on-property-pattern]]
