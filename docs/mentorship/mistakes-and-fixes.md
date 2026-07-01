---
name: mistakes-and-fixes
---

# Mistakes and Fixes

Every avoidable mistake from InterviewLab's build history, with root cause, fix, and the prevention rule it produced. The goal isn't a blameless log — it's making sure each mistake bought a permanent standing rule (see CLAUDE.md `NEVER DO THIS`) so it can't recur.

| # | Mistake | Root Cause | Fix | Prevention | Tokens Wasted (approx) |
|---|---|---|---|---|---|
| 1 | Gemini billing assumption | Free tier usage never verified against actual dev-time call volume before wiring Gemini as default provider | Switched default provider to Ollama (local, zero cost) — [[ADR-001-ollama-over-gemini]] | Never assume a free tier without verifying it first (CLAUDE.md Golden Rules) | ~30k |
| 2 | ZooKeeper added to `docker-compose.yml` | Copied the older, widely-tutorialized Kafka setup pattern instead of checking Kafka's current setup docs | Removed ZooKeeper, KRaft mode only — [[ADR-008-kraft-over-zookeeper]] | Never suggest ZooKeeper — Kafka uses KRaft mode only (CLAUDE.md `NEVER DO THIS`) | ~10k |
| 3 | CDN React import/export bugs | Wrote `export default` / `import` out of habit; CDN + in-browser Babel has no module system, only `window.*` globals | Rewired every component to attach to `window.ComponentName`, fixed script tag load order | CDN setup requires explicit `window.*` globals — ES modules don't work — [[ADR-003-cdn-react-over-bundled]] | ~15k |
| 4 | IPv4/IPv6 `localhost` mismatch | JVM on Windows resolved `localhost` to `::1`; Ollama only binds an IPv4 listener | Hardcoded `127.0.0.1` for `OLLAMA_BASE_URL` and all local-service connections | Never use `localhost` for JVM connections on Windows — use `127.0.0.1` — [[ADR-007-localhost-vs-127]] | ~10k |
| 5 | Spring AI property path (`options.model`) | Assumed `spring.ai.ollama.chat.options.model` was correct by analogy with other `options.*` properties; it binds silently but is never read | Switched to `spring.ai.ollama.chat.model`, confirmed via `javap` decompilation of `OllamaChatProperties` | Never use `spring.ai.ollama.chat.options.model` — verify Spring AI property paths against source before trusting docs — [[ADR-006-spring-ai-property-path]] | ~10k |
| 6 | `dotenv` Maven plugin dead end | Assumed a Maven plugin could auto-load `.env` before `spring-boot:run`, same as Node's `dotenv` — evaluated three candidates, none work on Java 25's module system | Abandoned the plugin approach; `start-local.ps1` loads `.env` into the shell process before `mvn spring-boot:run` — env vars inherit through Maven into the forked JVM | Never suggest `dotenv` Maven plugins — incompatible with Java 25 module system (CLAUDE.md `NEVER DO THIS`) | ~5k |
| 7 | `git init` at workspace root | Initialized git at `TheCodeForgeWorkspace/` root instead of the individual project directory — mixed unrelated projects into one repo history | Repo deleted, rebuilt scoped to `InterviewLab/` only | Always confirm the intended repo root before `git init` — never assume the current working directory is correct without checking | — |
| 8 | Workspace pushed to a public repo | A workspace-scoped git history (containing personal/internal content across multiple projects) was pushed to a public GitHub remote before scoping was corrected | Repo deleted, rebuilt clean, re-pushed scoped to `InterviewLab/` only with `.env`, `CLAUDE.md`, `session-notes/`, `.claude/`, `target/` excluded | Open Source Release Standard (CLAUDE.md) — verify repo scope and excluded files *before* the first public push, not after | — |

**Total avoidable token waste: ~80k** across mistakes 1–6 (mistakes 7–8 were git/process errors, not token-burning debug loops).

See also: [[session-learnings]], [[weak-areas]]
