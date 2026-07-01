# ADR-008: KRaft mode, no ZooKeeper

## Status
Accepted

## Context
`docker-compose.yml` initially included a ZooKeeper container alongside Kafka, following the older, widely-copy-pasted Kafka setup pattern from pre-4.x documentation and tutorials. ZooKeeper coordination for Kafka is deprecated as of Kafka 4.x — KRaft (Kafka's own built-in Raft-based consensus, stable since Kafka 3.3+) replaces it, and new Kafka setups have no reason to add ZooKeeper.

## Decision
Run Kafka in **KRaft mode** — `KAFKA_PROCESS_ROLES: broker,controller` with `KAFKA_CONTROLLER_QUORUM_VOTERS` pointing at itself, no ZooKeeper container, no ZooKeeper dependency anywhere in the stack. See `docker-compose.yml`'s `kafka` service.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| ZooKeeper-coordinated Kafka | Rejected | Deprecated as of Kafka 4.x — no reason to add a dependency the project's own roadmap is removing |
| **KRaft mode** | **Chosen** | Current standard, one fewer container, simpler local dev stack |

## Consequences
- One fewer container in `docker-compose.yml` — simpler local dev environment, faster `docker-compose up`, no ZooKeeper port/health check to manage
- Kafka itself is still parked V1 (`EventPublisher` sync in-process implementation is the runtime default — see [[ADR-004-conditional-on-property-pattern]]); the KRaft decision applies the moment Kafka is unparked, so there's no future migration debt waiting
- Documented as a standing rule in CLAUDE.md `NEVER DO THIS`: never suggest ZooKeeper for any new Kafka setup — Kafka uses KRaft mode only

## Lesson
Widely-copied tutorial patterns lag behind a project's own deprecation roadmap. Kafka's own documentation had already marked ZooKeeper coordination for removal — the fix wasn't a debugging exercise, it was checking the current official setup guide instead of trusting a commonly-copied `docker-compose.yml` snippet.

## Interview Talking Point
"KRaft mode is the current Kafka standard — ZooKeeper removal has been on Kafka's own roadmap since 2.8, stable since 3.3. I caught it in local dev before it became a real dependency, by checking Kafka's current setup docs instead of the ZooKeeper-based pattern that's still floating around in most tutorials and Stack Overflow answers."

See also: [[ADR-004-conditional-on-property-pattern]]
