# ADR-001: Ollama over Gemini as the default AI provider

## Status
Accepted

## Context
Gemini was the original V1 AI provider (`GeminiProvider`, calling Google's `generateContent` REST API directly via `RestClient` — no Spring AI dependency). Mid-development, Google AI Studio surfaced a billing prompt for continued use. The workspace Golden Rule is zero paid tools until traffic demands it, and the assumption that Gemini's free tier would cover V1 development traffic turned out to be wrong — free-tier limits are narrower and less predictable than assumed, and were never verified against actual call volume before Gemini was wired in as default.

This directly violated the standing rule: *never assume a free tier without verifying it first.*

## Decision
Switch the default AI provider to **Ollama**, running `llama3.2` locally, with zero per-request cost. `AiProviderStrategy` (Strategy pattern) already abstracted the provider behind `generate()` / `generateJson()` / `providerName()`, so the switch is a configuration change, not a rewrite.

- `app.ai.provider` / `AI_PROVIDER` env var now defaults to `ollama` (`application.yml`)
- `app.ai.default-provider` / `DEFAULT_AI_PROVIDER` (stored on new user profiles) also defaults to `ollama`
- `GeminiProvider` stays in the codebase, fully wired, selectable by setting `AI_PROVIDER=gemini` — nothing was deleted (Golden Rule: enhance always, remove never)

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Gemini | Rejected | Billing prompt appeared mid-development; free-tier assumption unverified |
| **Ollama (llama3.2, local)** | **Chosen** | Zero cost, runs entirely local, no external dependency for core interview loop |
| Claude | Rejected | Paid API, no free tier suitable for continuous development traffic |
| OpenAI | Rejected | Paid API, same reasoning as Claude |

## Consequences
- Zero marginal cost for all interview/mentor AI calls during development and V1 operation
- Cold start latency of ~30-40s on first request after Ollama has been idle (model load into memory) — mitigated by `pull-model-strategy: never` at startup (fails fast instead of blocking app boot) and a documented request timeout (`AI_REQUEST_TIMEOUT_SECONDS`, `app.ai.request-timeout-seconds`) enforced at the `RestClient` level in `WebMvcConfig`
- Weaker model quality than Gemini/Claude/GPT-class models for nuanced feedback generation — acceptable trade for V1; revisit when cost rule allows a paid provider
- All four providers (`OLLAMA`, `GEMINI`, `CLAUDE`, `OPENAI`) remain enumerated in `AiProvider` and routable through `AIProviderFactory` — switching back, or per-user provider preference (`user_profiles.preferred_ai_provider`), requires no code change

## Lesson
Never assume a free tier is actually free at your expected volume — verify against the provider's published limits and your projected call count *before* wiring it in as the default. When a provider swap is needed later, it should cost a config change, not a redesign — that's the point of the Strategy pattern being in place *before* the pain shows up, not after.

## Interview Talking Point
"I switched the default AI provider mid-project after Gemini's free tier turned out not to cover our usage. Because the provider was already behind a Strategy interface (`AiProviderStrategy`, selected by `AIProviderFactory`), the switch was a one-line env var change (`AI_PROVIDER=ollama`) — zero code change, zero downtime, and the old provider stayed available as a fallback rather than being ripped out."

See also: [[ADR-004-conditional-on-property-pattern]], [[weak-areas]]
