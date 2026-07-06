# ADR-011: Semaphore-based AI request queue + daily budget kill switch

## Status
Accepted

## Context
Every AI provider call (Ollama, Gemini, and parked Claude/OpenAI) went straight from the calling service to `AIProviderFactory.getDefaultProvider()...generate()` with no concurrency limit and no usage cap. Two distinct risks follow from that: (1) a burst of concurrent requests against a local Ollama instance (single-threaded-ish, slow, no autoscaling) or a rate-limited Gemini key could pile up unbounded in-flight calls with no back-pressure; (2) nothing stood between a bug (an infinite retry loop, a misbehaving agent chain) or an abuse pattern and an unbounded daily API bill — concurrency alone doesn't catch a low-concurrency-but-high-volume-over-a-day pattern.

## Decision
Two independent, composable guards, both threaded through a single choke point:
- **AIRequestQueue** — a fair `java.util.concurrent.Semaphore` sized to `app.ai.queue.max-concurrent`. `tryAcquire` with a timeout (`app.ai.queue.timeout-seconds`); on timeout, throws `AIProviderException(AI_BUSY, 429)` carrying the timeout value so `GlobalExceptionHandler` can set `Retry-After`.
- **AiBudgetGuard** — a `SessionStore`-backed global daily counter (`ai:budget:{date}`, 25h TTL — one hour of slack past the 24h window so a slightly-late request in the final hour of the day still reads the same day's count rather than silently resetting). Breach throws `AIProviderException(AI_BUDGET_EXHAUSTED, 503)` and logs `[AI_BUDGET_ALERT]` — an alert-friendly marker for log-based monitoring/paging, not a metrics integration (none exists yet).
- **QueuedAiProviderStrategy** — a Decorator wrapping whatever `AiProviderStrategy` `AIProviderFactory.getProvider()`/`getDefaultProvider()` would have returned. Every `generate()`/`generateJson()` call passes through the queue first, and the budget guard increments only *after* a queue permit is acquired — a request rejected as `AI_BUSY` never spends budget it didn't actually use. This is the single choke point: no per-provider (`OllamaProvider`, `GeminiProvider`, ...) duplication of either concern, and no calling service needed to change.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Reactive backpressure (WebFlux/Reactor `Flux` with `onBackpressureBuffer`/limitRate) | Rejected | The whole stack is Spring MVC + virtual threads, not WebFlux — introducing a reactive type just for this one cross-cutting concern would mean either a parallel reactive stack or an awkward block-on-Mono bridge, for no benefit at this traffic scale |
| Per-provider rate limiting (each of Ollama/Gemini/Claude/OpenAI enforces its own limit) | Rejected | Duplicates the same logic 4x, and a provider added later would silently lack the protection unless someone remembered to add it there too |
| **Semaphore + SessionStore counter, wrapped once in the factory** | **Chosen** | A blocking `Semaphore` is the simplest correct primitive for "N concurrent slots" on a thread-per-request (virtual threads) server — no reactor, no manual queue/executor management. Wrapping once in `AIProviderFactory` guarantees every current and future provider gets both guards for free |

## Consequences
- `AIRequestQueue` uses a *fair* semaphore (FIFO) — under sustained contention, no request starves behind newer arrivals, at a small throughput cost versus an unfair semaphore. Acceptable given max-concurrent defaults to 5.
- The budget counter is genuinely global (not per-user) — by design, since the threat model is "the whole app's daily AI bill," not "one user's fair share" (that's `RateLimitService`'s job, see ADR-010).
- `QueuedAiProviderStrategy` had to expose a package-private `delegate()` accessor purely for `AIProviderFactoryTest`, which previously asserted `isSameAs()` on the raw provider mock — wrapping every returned provider broke that identity check, and unwrapping via `delegate()` was simpler than restructuring the test around behavioral (stub-and-verify) assertions.
- `AI_BUSY`'s `Retry-After` header value is carried on the exception itself (`AIProviderException.retryAfterSeconds()`), not read from a config bean inside `GlobalExceptionHandler` — injecting `AiQueueProperties` there would have forced every `@WebMvcTest` slice in the suite (there are eleven) to load a `@ConfigurationProperties` bean they have no other reason to need, since `@WebMvcTest` doesn't process `@ConfigurationPropertiesScan` from the main application class.

## Lesson
The blast radius of a change isn't always where you'd expect it. Adding a queue and a budget guard "in front of" AI calls sounds self-contained, but the naive version broke an existing identity-based test assertion and would have broken eleven unrelated controller test slices if the Retry-After value had been wired as a constructor dependency into the global exception handler instead of carried on the exception. Threading a new cross-cutting concern through existing infrastructure means checking every place that infrastructure is already exercised in tests, not just the new code path.

## Interview Talking Point
"I added a request queue and a daily budget kill switch in front of every AI provider call, wrapped once in the factory as a Decorator so no individual provider or calling service had to change. The two failure modes are deliberately different HTTP semantics: `AI_BUSY` is 429 with `Retry-After` — a transient, retryable condition — while `AI_BUDGET_EXHAUSTED` is 503 — a hard stop for the rest of the day, log-marked for alerting. I picked a plain `Semaphore` over reactive backpressure because the app runs on Spring MVC with virtual threads, not WebFlux, and a blocking semaphore is the simplest correct primitive for bounding concurrency on that model — reaching for Reactor would have meant fighting the framework, not using it."

See also: [[ADR-004-conditional-on-property-pattern]], [[ADR-009-sessionstore-abstraction]], [[ADR-010-fixed-window-ratelimit]]
