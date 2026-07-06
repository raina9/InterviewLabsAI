# ADR-010: Fixed-window rate limiting, not sliding window

## Status
Accepted

## Context
`RateLimitService` needed to enforce `DAILY_LIMIT` per user, backed by the new `SessionStore` abstraction (ADR-009) so it works identically under the in-memory default and under Redis once `DEPLOYMENT_MODE=production` requires it. The natural Redis implementation of a sliding-window rate limiter (a sorted set of request timestamps, pruned and counted on every check) costs multiple Redis commands per request — typically a `ZREMRANGEBYSCORE` + `ZADD` + `ZCARD` + `EXPIRE`, four round trips minimum. Upstash's free tier bills and caps by command count, not by connection or bandwidth, so a sliding window would burn through the free-tier command budget roughly 4x faster than necessary for a feature (daily interaction cap) that doesn't need sliding-window precision in the first place.

## Decision
`RateLimitService` uses a fixed window keyed by calendar day: `ratelimit:{userId}:{LocalDate.now()}`. Each request calls `SessionStore.increment(key, 24)` — one atomic operation. The TTL is applied only when the key is first created (count transitions from absent to `1`); subsequent increments within the same day do not re-touch the expiry. This is exactly one Redis command (`INCR`) per request in the steady state, with a second command (`EXPIRE`) only on the single request per day that creates the window.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Sliding window (sorted set of timestamps) | Rejected | Needs 3-4 Redis commands per check; precise but the precision isn't a product requirement for a daily cap, and it isn't affordable on Upstash's free-tier command budget |
| Token bucket | Rejected | Solves burst-smoothing, which isn't the problem here — the requirement is a hard daily ceiling, not a steady request rate |
| **Fixed window, single INCR + conditional EXPIRE** | **Chosen** | One command per request in steady state; the boundary-burst imprecision (a user could in theory send 2x the limit across a midnight boundary) is an accepted tradeoff for a feature this coarse-grained |

## Consequences
- `SessionStore.increment()` had to be added as a fourth method beyond the original `put`/`get`/`delete` contract specifically to make this atomic under Redis (`RedisTemplate.opsForValue().increment()` maps directly to Redis's native `INCR`) — a naive `get` + add 1 + `put` would race under concurrent requests from the same user.
- The known fixed-window weakness (a user can send up to ~2x `DAILY_LIMIT` requests clustered around a local-midnight boundary) is accepted here because the feature being protected is a coarse usage cap, not a precise billing meter.
- Window boundary is the JVM's default time zone via `LocalDate.now()`, not UTC — a user's count resets at their server's local midnight. Acceptable for a single-region deployment; would need revisiting if InterviewLab ever deploys across multiple time zones with per-region limit resets expected.
- `RateLimitException` maps to `429 Too Many Requests` via `GlobalExceptionHandler`, consistent with the existing per-domain exception pattern (`QuizException`, `DrillException`, etc.).

## Lesson
Command budget, not just latency, is a real constraint on managed free-tier infrastructure — a technically "more correct" sliding window algorithm can be the wrong engineering choice when the actual requirement (a coarse daily cap) doesn't need that precision and the cost model punishes the extra round trips. Matching the algorithm to the actual precision requirement, not the theoretically best one, is what kept this feature inside Upstash's free tier.

## Interview Talking Point
"I chose a fixed-window rate limiter over a sliding window specifically because of Upstash's free-tier command-based billing — a sliding window needs 3-4 Redis commands per check (prune + add + count + expire), while a fixed window is a single `INCR` per request with `EXPIRE` set only once per window. The tradeoff is that a user can burst up to roughly 2x the limit right at the day boundary, which I accepted because the feature is a coarse daily usage cap, not a precise rate meter — the algorithm should match the actual precision the product needs, not the theoretically most correct one."

See also: [[ADR-009-sessionstore-abstraction]]
