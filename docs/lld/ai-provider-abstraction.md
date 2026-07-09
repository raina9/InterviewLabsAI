# AI Provider Abstraction — LLD

Source-cited low-level design of `AiProviderStrategy`, `AIProviderFactory`, and the
`QueuedAiProviderStrategy` decorator that sits between every caller and every provider.

## Plain English

Every place in the codebase that needs to call an LLM goes through one factory method
and one interface — it never talks to Ollama or Gemini directly, and it never has to
know about concurrency limits or daily budgets either. The factory decides which
concrete provider to instantiate based on a `switch` over an enum; a decorator wraps
whatever it returns so that queueing and budget-checking happen exactly once, in exactly
one place, for every provider.

## Request Path — One AI Call

```
Caller (InterviewAgent / MentorAgent / QuizService / ...)
  │
  ▼
AIProviderFactory.getProvider(AiProvider.OLLAMA)   or   .getDefaultProvider()
  │
  ▼
switch (type) { case OLLAMA -> ollamaProvider; case GEMINI -> geminiProvider; ... }
  │
  ▼
new QueuedAiProviderStrategy(delegate, aiRequestQueue, aiBudgetGuard)   ◄── always wrapped
  │
  ▼
caller.generate(prompt, options)  /  caller.generateJson(prompt, options)
  │
  ▼
QueuedAiProviderStrategy.generate()
  │
  ▼
AIRequestQueue.execute(() -> {
    AiBudgetGuard.checkAndIncrement();
    return delegate.generate(prompt, options);   ◄── real provider call happens here
})
```

Source: `AIProviderFactory.getProvider()` (`ai/AIProviderFactory.java:58-70`),
`QueuedAiProviderStrategy.generate()` (`ai/QueuedAiProviderStrategy.java:22-27`).

## Strategy Pattern — `AiProviderStrategy`

```java
public interface AiProviderStrategy {
    String generate(String prompt, AIOptions options);
    String generateJson(String prompt, AIOptions options);
    AiProvider providerName();
}
```

Named `AiProviderStrategy`, not `AIProvider`, specifically to avoid a file-system
collision with the `AiProvider` enum on case-insensitive filesystems (Windows, default
macOS) — both would resolve to `AiProvider.java`. Exhaustiveness is enforced by the
`AiProvider` enum switch in `AIProviderFactory`, not by a `sealed` modifier on the
interface itself — `sealed` was tried and removed because Mockito cannot generate proxies
for sealed types, which broke every test that mocks `AiProviderStrategy`
([[decisions/ADR-005-sealed-interface-removed]]).

| Implementation | Status | Mechanism |
|---|---|---|
| `OllamaProvider` | **Active (default)** | Spring AI `OllamaChatModel`, local `llama3.2` — [[decisions/ADR-001-ollama-over-gemini]], [[decisions/ADR-006-spring-ai-property-path]] |
| `GeminiProvider` | Wired, parked | Direct `RestClient` call to Google's `generateContent` REST API — no Spring AI dependency, hand-rolled request/response mapping |
| `ClaudeProvider` | Parked | `@ConditionalOnProperty(app.ai.provider=claude)` — bean not created unless configured |
| `OpenAIProvider` | Parked | Same pattern as Claude |

`AIProviderFactory` receives `ClaudeProvider`/`OpenAIProvider` as `Optional<T>`
(constructor injection) — startup never fails on a missing bean, and selecting an
unconfigured parked provider throws `IllegalStateException` only at call time, with a
message naming the exact env vars needed (`ai/AIProviderFactory.java:63-66`).

## Decorator Pattern — `QueuedAiProviderStrategy`

Package-private, only constructible by `AIProviderFactory` — no other code in the
codebase can bypass the queue/budget wrapping, since nothing outside the `ai` package
can reach a raw provider instance:

```java
final class QueuedAiProviderStrategy implements AiProviderStrategy {
    private final AiProviderStrategy delegate;
    private final AIRequestQueue     aiRequestQueue;
    private final AiBudgetGuard      aiBudgetGuard;
    // generate()/generateJson() both route through aiRequestQueue.execute(() -> {
    //     aiBudgetGuard.checkAndIncrement();
    //     return delegate.generate(prompt, options);
    // });
}
```

Why a Decorator instead of putting queue/budget logic in each provider: four providers
would each need to duplicate the same Semaphore-acquire / budget-check / release
sequence, and a fifth provider added later could easily forget it. Wrapping once in the
factory makes the guard structural, not something every new `AiProviderStrategy`
implementation has to remember. `providerName()` delegates straight through so callers
that branch on `providerName()` see no behavioral difference from an unwrapped provider.
A package-private `delegate()` accessor exists solely so `AIProviderFactoryTest` can
assert routing identity without stubbing `providerName()` on every mock.

Full behavior of `AIRequestQueue` (Semaphore, `AI_BUSY` 429) and `AiBudgetGuard` (daily
global cap, `AI_BUDGET_EXHAUSTED` 503): [[ai-architecture]] → "Cost & Concurrency
Control", [[decisions/ADR-011-ai-request-queue]].

## Switch Mechanism

`AI_PROVIDER` env var → `app.ai.provider` → `AiProperties.defaultProvider()` →
`AIProviderFactory.getDefaultProvider()`. No code change required to move between
providers that are already wired (`OLLAMA`, `GEMINI`); `CLAUDE`/`OPENAI` additionally
need their `@ConditionalOnProperty` value to match and their API key env var set.

## Known Gap

`user_profiles.preferred_ai_provider` is written on profile creation
(`DEFAULT_AI_PROVIDER` / `app.ai.default-provider`) but never read back —
`getDefaultProvider()` always resolves from `AiProperties`, not per-user. Per-user
provider preference is schema-ready, not wired. See [[ai-architecture]].

See also: [[agent-architecture]], [[swappable-backend-pattern]], [[ai-architecture]]
