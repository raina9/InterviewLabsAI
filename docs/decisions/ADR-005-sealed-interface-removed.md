# ADR-005: Sealed interface removed from AiProviderStrategy

## Status
Accepted

## Context
`AiProviderStrategy` was originally declared `sealed` with a `permits` clause listing `OllamaProvider, GeminiProvider, ClaudeProvider, OpenAIProvider`, intended to give the compiler exhaustiveness checking wherever the interface was pattern-matched. In practice, the interface is not pattern-matched anywhere — it's injected and called polymorphically (`provider.generate(...)`) — and Mockito could not create a mock/proxy for the sealed type in unit tests, since sealed types restrict subclassing to the explicitly permitted set and Mockito's proxy generation needs to produce a new subclass at test time.

## Decision
Remove `sealed`/`permits` from `AiProviderStrategy`; it is now a plain interface. Exhaustiveness is instead enforced where it actually matters: the `AiProvider` enum switch inside `AIProviderFactory.getProvider(AiProvider type)`, which the compiler *does* check exhaustively for a `switch` over an enum (Java requires either a `default` or all enum constants covered).

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Keep sealed + Mockito workaround (e.g. `mockito-inline`, manual test double) | Rejected | Adds test infrastructure complexity to work around a design choice that wasn't earning its keep |
| **Remove sealed from the interface, keep exhaustiveness at the enum switch** | **Chosen** | Interface is injected, not pattern-matched — sealed was protecting the wrong thing |

## Consequences
- Lost compile-time exhaustiveness *on the interface itself* — nothing stops a future rogue implementation of `AiProviderStrategy` outside the `ai` package
- Kept compile-time exhaustiveness where it's actually exercised: `AIProviderFactory`'s `switch (type)` over the `AiProvider` enum still fails to compile if a new enum constant is added without a corresponding `case`
- Full Mockito mockability restored for `AiProviderStrategy` in unit tests — no test infrastructure workaround needed

## Lesson
Use `sealed` for types you pattern-match on (where the compiler's exhaustiveness check earns its keep), not for interfaces you inject and call polymorphically. The two situations look similar — "closed set of implementations" — but only one of them benefits from `sealed`, and the other one just breaks Mockito.

## Interview Talking Point
"I initially sealed `AiProviderStrategy` for compile-time exhaustiveness, then removed it when I discovered Mockito can't generate a proxy for a sealed type in unit tests. The interface was never actually pattern-matched — it's injected and called polymorphically — so exhaustiveness properly belongs on the enum switch in the factory, not the interface itself. Sealed is for the types you switch over, not the ones you inject."
