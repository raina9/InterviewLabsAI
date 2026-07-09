# Interview Session Lifecycle — LLD

Source-cited low-level design of the `Session` state machine, ownership enforcement, and
how a session's status interacts with the interview turn loop.

## Plain English

A session is created once, then accepts a fixed sequence of question/answer turns while
`ACTIVE`. It ends in exactly one of two terminal states: `COMPLETED` (the candidate
finished, either by hitting the question count or clicking Finish) or `ABANDONED`
(the candidate walked away early). Once terminal, a session is read-only — no further
turns, no re-activation. Every write path checks that the requesting user owns the
session before touching it.

## State Machine

```
        SessionService.createSession()
               │
               ▼
            ACTIVE ────────────────► COMPLETED
               │        completeSession()          (terminal)
               │        — manual (POST /{id}/complete)
               │        — automatic (InterviewService.respond(),
               │            turnResult.shouldMoveToNextQuestion()==true)
               │
               └────────────────────► ABANDONED
                        abandonSession()             (terminal)
                        (POST /{id}/abandon)
```

`SessionStatus` (`session/SessionStatus.java`): `ACTIVE`, `COMPLETED`, `ABANDONED` —
stored as `TEXT` via `@Enumerated(EnumType.STRING)`. No `CANCELLED`, no `PAUSED` — the
model is intentionally two-terminal-state, not a richer workflow.

Both transitions are guarded by `assertActive()` in `SessionService`
(`session/SessionService.java:92-99`) — attempting either transition on a non-`ACTIVE`
session throws `SessionException(SESSION_NOT_ACTIVE, 409)`. There is no path back to
`ACTIVE` from either terminal state.

## Auto-Completion — a Non-Obvious Coupling

`InterviewService.respond()` (`interview/InterviewService.java:107-109`) calls
`sessionService.completeSession()` directly when
`turnResult.shouldMoveToNextQuestion()` is `true` — reusing the exact same status
transition and `session.completed` event as the manual **Finish** button
(`SessionController.completeSession()`), so completion behavior is identical regardless
of which path triggered it. **Current caveat:** `InterviewAgent.nextTurn()` always
returns `shouldMoveToNextQuestion=false` in V1 (question-progression logic is not yet
implemented — see [[ai-architecture]]), so in practice today every session reaches
`COMPLETED` only through the manual Finish button, never automatically. The wiring is in
place; the agent-side trigger is a documented V2 gap.

## Ownership Enforcement

Every read and write path repeats the same two-step guard —
`findSessionOrThrow()` then `assertOwnership()` — independently in both
`SessionService` and `InterviewService` (not shared via a common base, each service owns
its copy):

```java
private void assertOwnership(Session session, UUID userId) {
    if (!session.getUserId().equals(userId)) {
        throw new SessionException(SESSION_ACCESS_DENIED, HttpStatus.FORBIDDEN, ...);
    }
}
```

A session belonging to another user returns 403 `SESSION_ACCESS_DENIED`, never a 404 —
the session's existence is not hidden, only access to it. Full endpoint-level
authorization table: [[authz-matrix]].

## Request Path — Session Endpoints

```
SessionController (/api/v1/sessions)
  POST            /            → createSession()   → Session(ACTIVE)
  GET             /            → getUserSessions()  → own sessions only
  GET             /{id}        → getSession()       → ownership-checked
  POST            /{id}/complete → completeSession() → ACTIVE → COMPLETED
  POST            /{id}/abandon  → abandonSession()  → ACTIVE → ABANDONED
  GET             /{id}/messages → getMessages()     → ownership-checked (via getSession() call, result discarded)
```

`getMessages()` calls `sessionService.getSession(id, principal.id())` purely for its
ownership-check side effect and discards the returned `Session` — the actual data comes
from `MessageService.getSessionMessages()` (`session/SessionController.java:82-89`).

## Events

`session.completed` is published via `EventPublisher.publishSessionCompleted(sessionId)`
on every `COMPLETED` transition (manual or auto), `SyncEventPublisher` (in-process) in
V1 — see [[swappable-backend-pattern]]. `abandonSession()` publishes no event.

See also: [[agent-architecture]], [[authz-matrix]], [[ai-architecture]]
