# ADR-007: 127.0.0.1, never localhost, for JVM connections on Windows

## Status
Accepted

## Context
`OllamaProvider` connects to a locally running Ollama instance. Configuring the base URL as `http://localhost:11434` worked fine from `curl` and from a browser, but failed from the JVM with a connection error. On Windows, `localhost` can resolve to `::1` (IPv6 loopback) ahead of `127.0.0.1` (IPv4 loopback) depending on the JVM's resolver configuration and network stack state, while Ollama's local server binds only an IPv4 listener. `curl` and browsers on the same machine happened to resolve or fall back differently, masking the mismatch until the JVM tried it.

## Decision
Always use the literal `127.0.0.1` — never the `localhost` hostname — for any local service the JVM connects to on Windows. Applied to `OLLAMA_BASE_URL` (`application.yml`: `${OLLAMA_BASE_URL:http://127.0.0.1:11434}`) and documented as a standing rule in CLAUDE.md `NEVER DO THIS`.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| `localhost` | Rejected | Resolves to IPv6 (`::1`) on this JVM/Windows combination; Ollama has no IPv6 listener → connection failure |
| **`127.0.0.1`** | **Chosen** | Forces IPv4 explicitly, matches Ollama's actual bound listener |

## Consequences
- Every local-service base URL in config (`application.yml`, `.env.example`, `start-local.ps1`) uses `127.0.0.1`, not `localhost`, by convention going forward
- One extra thing to remember when adding a new local dependency on Windows: check `netstat` for which family it actually binds before assuming `localhost` will resolve correctly for the JVM

## Lesson
`curl` succeeding is not proof that `localhost` resolves correctly for every process — different tools and runtimes can have different IPv4/IPv6 resolution order or fallback behavior on the same machine. When a service is reachable from one client but not another on `localhost`, suspect the resolver, not the service, and verify with `netstat` which address family is actually listening.

## Interview Talking Point
"I hit a `localhost` connection failure that only reproduced from the JVM, not from `curl` or the browser, on the same Windows machine. Debugged it with `netstat` to confirm Ollama only had an IPv4 listener bound, while the JVM was resolving `localhost` to the IPv6 loopback address first. Fixed it by hardcoding `127.0.0.1` for all local-service connections and made it a standing rule for the project, not a one-off patch."
