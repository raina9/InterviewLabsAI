# ADR-006: spring.ai.ollama.chat.model, not .chat.options.model

## Status
Accepted

## Context
Spring AI 2.0's Ollama starter exposes what looks like two plausible property paths for setting the chat model: `spring.ai.ollama.chat.model` and `spring.ai.ollama.chat.options.model`. The `options.*` path looked more "correct" by analogy with other Spring AI option properties (temperature, top-p, etc. do live under `options.*`) — but setting it had no effect. Requests silently continued using Ollama's own default model, with no error, no warning, no indication the property was being ignored.

## Decision
Use `spring.ai.ollama.chat.model` (the flattened path), confirmed by decompiling Spring AI's `OllamaChatProperties` with `javap`: the `model` field is a **direct field** on `OllamaChatProperties`, and `toOptions()` reads that direct field — it does **not** read the `model` field on the nested `Options` inner class that `options.*` binds to. Setting `options.model` binds successfully (Spring's relaxed binding doesn't complain) but the value is simply never consulted by the code path that builds the actual chat request.

```yaml
spring:
  ai:
    ollama:
      chat:
        model: ${OLLAMA_MODEL:llama3.2}   # correct — read by toOptions()
        # options:
        #   model: ...                     # binds silently, never read — dead config
```

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| `spring.ai.ollama.chat.options.model` | Rejected | Dead configuration — binds without error but is never read by `toOptions()` |
| **`spring.ai.ollama.chat.model`** | **Chosen** | Verified via bytecode decompilation to be the field `toOptions()` actually reads |

## Consequences
- The correct path is now the only one used and is documented inline in `application.yml` with the `javap` finding preserved as a comment, so the mistake can't silently resurface
- Documented in CLAUDE.md `NEVER DO THIS`: never use `spring.ai.ollama.chat.options.model`
- General caution established: verify Spring AI property paths against source/bytecode when documentation is ambiguous, rather than trusting the property naming pattern to be consistent across the library

## Lesson
When a Spring Boot property binds without any error but produces no observable effect, treat it as a silent-failure config bug, not "must be something else wrong." `javap` on the compiled `@ConfigurationProperties` class is a fast, reliable way to see exactly which fields a binding path resolves to and which ones a downstream method (`toOptions()`, here) actually reads — the property binding succeeding is not proof the value is used.

## Interview Talking Point
"I hit a case where a Spring AI config property bound successfully — no startup error, no validation failure — but had zero effect on the actual model used at request time. Instead of guessing, I decompiled the `@ConfigurationProperties` class with `javap` and found the property was binding to a nested `Options` object that the request-building method never reads. Root-caused it to the bytecode level rather than trial-and-error on YAML."
