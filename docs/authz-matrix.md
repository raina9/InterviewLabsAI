# Authorization Matrix

## Current Role Model — Gap Notice

**There is no role-based access control in InterviewLab.** No `ADMIN` role, no `CANDIDATE` role, no role field on `User` at all. `SecurityConfig` distinguishes exactly two states: **authenticated** and **unauthenticated**. Every authenticated principal — regardless of who they are — has identical access to every `/api/v1/**` endpoint. There is no admin surface, no per-endpoint role check, no `@PreAuthorize`/`hasRole()` anywhere in the codebase.

This matrix therefore documents the *actual* current state (single implicit role, informally "CANDIDATE") plus the endpoint-level authentication requirement, and calls out `ADMIN` as unimplemented rather than pretending it exists. See [[mentorship/weak-areas]] for why this is flagged as a real interview-prep gap, not just a doc nicety.

## Auth Modes (SecurityConfig)

| Mode | Trigger | Mechanism |
|---|---|---|
| `dev` | `AUTH_MODE=dev` (default) or no Google credentials configured | `DevTokenFilter` — `X-Dev-Token` header compared against `app.auth.dev-token`; match sets a fixed dev principal in `SecurityContext` |
| `oauth` | `AUTH_MODE=oauth`, or `AUTH_AUTO_DETECT=true` (default) + `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` both set | Google OAuth2 login (`oauth2Login`) → `OAuth2SuccessHandler` issues a JWT → `JwtAuthFilter` validates the JWT httpOnly cookie on subsequent requests |

Fail-closed default: `anyRequest().denyAll()` — any path not explicitly matched is rejected, not implicitly allowed.

## Endpoint Matrix

Role column reflects the only role that currently exists — every authenticated user, informally "CANDIDATE." `ADMIN` is listed as **N/A — not implemented** for every row since no admin surface exists anywhere in the API.

| Endpoint | Method | Auth Required | CANDIDATE (all authenticated users) | ADMIN |
|---|---|---|---|---|
| `/api/v1/auth/me` | GET | Public route, but 401 thrown programmatically if principal is null | Own profile only (JWT claims) | N/A — not implemented |
| `/api/v1/auth/logout` | POST | No (clears cookie regardless) | Clears own session cookie | N/A |
| `/api/v1/profile` | GET | Yes | Own profile only | N/A |
| `/api/v1/profile` | PUT | Yes | Own profile only | N/A |
| `/api/v1/profile/resume` | PUT | Yes | Own profile only | N/A |
| `/api/v1/profile/custom-prompt` | PUT | Yes | Own profile only | N/A |
| `/api/v1/sessions` | POST | Yes | Creates a session owned by self | N/A |
| `/api/v1/sessions` | GET | Yes | Lists own sessions only | N/A |
| `/api/v1/sessions/{id}` | GET | Yes | Own session only (ownership check in service layer) | N/A |
| `/api/v1/sessions/{id}/complete` | POST | Yes | Own session only | N/A |
| `/api/v1/sessions/{id}/abandon` | POST | Yes | Own session only | N/A |
| `/api/v1/sessions/{id}/messages` | GET | Yes | Own session only | N/A |
| `/api/v1/interview/start` | POST | Yes | Starts own interview | N/A |
| `/api/v1/interview/{sessionId}/respond` | POST | Yes | Own session only | N/A |
| `/api/v1/interview/{sessionId}/feedback` | GET | Yes | Own session only | N/A |
| `/api/v1/assessment/start` | POST | Yes | Own assessment | N/A |
| `/api/v1/assessment/submit` | POST | Yes | Own assessment | N/A |
| `/api/v1/assessment/report/{userId}` | GET | Yes | **Path-parameter `userId`, not derived from principal — see gap below** | N/A |
| `/api/v1/curriculum/{userId}` | GET | Yes | **Path-parameter `userId`, not derived from principal — see gap below** | N/A |
| `/api/v1/quiz/start` | POST | Yes | Own quiz session | N/A |
| `/api/v1/quiz/{sessionId}/answer` | POST | Yes | Own quiz session | N/A |
| `/api/v1/quiz/{sessionId}/result` | GET | Yes | Own quiz session | N/A |
| `/api/v1/code/challenge` | POST | Yes | Own challenge | N/A |
| `/api/v1/code/submit` | POST | Yes | Own submission | N/A |
| `/api/v1/code/challenge/{id}/hint` | GET | Yes | Own challenge | N/A |
| `/api/v1/drill/start` | POST | Yes | Own drill session | N/A |
| `/api/v1/drill/{sessionId}/next` | POST | Yes | Own drill session | N/A |
| `/api/v1/drill/{sessionId}/summary` | GET | Yes | Own drill session | N/A |
| `/api/v1/english/analyze` | POST | Yes | Own transcript | N/A |
| `/api/v1/voice/transcript` | POST | Yes | Own transcript | N/A |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | No (`permitAll`) | Public in dev — restrict in prod via env config | N/A |
| `/actuator/health` | GET | No (`permitAll`) | Required unauthenticated for Railway health checks | N/A |
| `/`, `/index.html`, `/static/**` | GET | No (`permitAll`) | Embedded frontend static assets | N/A |

## Known Gaps

1. **`{userId}` path parameters are not verified against the authenticated principal.** `/api/v1/assessment/report/{userId}` and `/api/v1/curriculum/{userId}` accept `userId` as a raw path variable. Nothing in `SecurityConfig` or (per current source review) the controller/service layer confirms `userId == principal.id()` before returning data — an authenticated user could request another user's `userId` and, if the row exists, potentially read their assessment report or curriculum. This is an IDOR-shaped gap and should be the first thing fixed if this matrix is being read as a pre-launch checklist, not just documentation.
2. **No `ADMIN` role exists.** Any future admin surface (user management, content moderation, system-wide analytics) has no authorization scaffolding to plug into yet — it would need a `role` column on `User`, `@PreAuthorize`/`hasRole()` checks, and explicit `SecurityConfig` matcher rules added from scratch.
3. **API documentation is `permitAll` in all environments** (no environment-based restriction currently implemented despite the comment noting it should be prod-restricted).

See also: [[mentorship/weak-areas]], [[hld]]
