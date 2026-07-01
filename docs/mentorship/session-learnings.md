---
name: session-learnings
---

# Session Learnings

Key transferable learnings from InterviewLab's major debugging sessions — the methodology, not just the fix. These are the skills that generalize to the next unrelated bug, not just this codebase.

## Ollama IPv4/IPv6 debug — network layer debugging methodology
When a service is reachable from one client (`curl`, browser) but not another (JVM) on the *same machine*, the service is not the first suspect — the resolver and the client's network stack are. Methodology used: confirm the service is actually listening with `netstat`, check which address family it's bound to (IPv4 vs IPv6), then check what the failing client actually resolves the hostname to before assuming the service is misconfigured. See [[ADR-007-localhost-vs-127]].

## Spring AI property path — bytecode decompilation as a debugging tool
When a Spring Boot `@ConfigurationProperties` binding succeeds (no startup error, no validation failure) but has no observable runtime effect, the binding succeeding is not proof the value is being read anywhere. `javap` on the compiled properties class shows exactly which fields exist and which nested objects a binding path resolves into — from there, tracing which method (`toOptions()`, here) actually reads which field settles the question definitively instead of guessing via trial-and-error YAML edits. See [[ADR-006-spring-ai-property-path]].

## CDN React — ES modules vs global scope
There is no module resolution in a CDN + in-browser Babel setup — `import`/`export` syntax either does nothing or throws at runtime, silently, with a stack trace that doesn't point at the real problem (a missing global, not a syntax error). Every component must explicitly assign itself to `window.ComponentName`, and every consuming script tag must load in dependency order by hand, since there's no bundler to resolve the graph. See [[ADR-003-cdn-react-over-bundled]].

## Flyway seed migration — JVM process restart required
Flyway resolves and applies migrations at application context startup. Adding a new migration file while the Spring Boot process is already running (e.g. via a hot-reload dev loop) does not pick it up — a full JVM restart is required for Flyway to see and apply a newly added `V*__description.sql` file. This is easy to mistake for a migration bug when it's actually a stale-running-process issue.

## Sealed interface removal — Mockito proxy generation requires non-sealed types
Mockito's `mock()` generates a dynamic subclass proxy of the target type at test time. A `sealed` interface restricts its permitted subtypes to an explicit `permits` list at compile time — Mockito's generated proxy class is not in that list, so mocking a sealed interface fails. This only surfaces when you try to unit-test code that depends on the sealed type via a mock, which can be well after the type was originally declared sealed. See [[ADR-005-sealed-interface-removed]].

See also: [[mistakes-and-fixes]], [[concepts-mastered]]
