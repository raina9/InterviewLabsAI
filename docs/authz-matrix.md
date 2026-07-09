# Authorization Matrix

## Current Role Model

`Role` enum (`CANDIDATE`, `ADMIN`) is stored on `users.role` (`@Enumerated(EnumType.STRING)`, `V9__add_user_role.sql`). `CANDIDATE` is the default for every OAuth-created and dev-mode-created user; `ADMIN` is granted only by a manual direct DB update — there is no self-service promotion endpoint (the seeded dev user is `ADMIN` so local dev has an admin account for free).

There is still no `@PreAuthorize`/`hasRole()` anywhere and no `@EnableMethodSecurity` — `SecurityConfig` itself only distinguishes **authenticated** vs **unauthenticated** at the filter-chain level. Role enforcement for the two admin endpoints is an **explicit in-controller check** (`requireAdmin(principal)` comparing `principal.role() != Role.ADMIN`, throwing 403 `FORBIDDEN_ADMIN_ONLY`) — a deliberate choice documented in both controllers' javadoc: adding `@EnableMethodSecurity` + AOP proxying for two check points is more moving parts than the checks themselves. Every other endpoint has no role gate at all — any authenticated `CANDIDATE` or `ADMIN` has identical access to their own data.

See [[mentorship/weak-areas]] for why "in-controller checks, not framework-level" is flagged as a real interview-prep discussion point, not just a doc nicety.

## Auth Modes (SecurityConfig)

| Mode | Trigger | Mechanism |
|---|---|---|
| `dev` | `AUTH_MODE=dev` (default) or no Google credentials configured | `DevTokenFilter` — `X-Dev-Token` header compared against `app.auth.dev-token`; match sets a fixed dev principal in `SecurityContext` |
| `oauth` | `AUTH_MODE=oauth`, or `AUTH_AUTO_DETECT=true` (default) + `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` both set | Google OAuth2 login (`oauth2Login`) → `OAuth2SuccessHandler` issues a JWT → `JwtAuthFilter` validates the JWT httpOnly cookie on subsequent requests |

Fail-closed default: `anyRequest().denyAll()` — any path not explicitly matched is rejected, not implicitly allowed.

### Dev-token behavior per deployment mode

`DeploymentProperties.mode` (`app.deployment.mode`) is independent of `AUTH_MODE` but constrains it: `DeploymentModeValidator` runs once on `ApplicationReadyEvent` and, only when `DEPLOYMENT_MODE=production`, throws `IllegalStateException` at startup if `AUTH_MODE=dev` — production must run `oauth`, full stop. `personal` mode (the default) runs no such check, so `AUTH_MODE=dev` (the `X-Dev-Token` full-trust bypass) is allowed by design for single-user local/relaxed use. As defense in depth beyond that event-listener check — which fires *after* `SecurityConfig`'s `@Bean` method has already run — `SecurityConfig.securityFilterChain()` itself also refuses to wire `DevTokenFilter` into the chain at all when `deploymentProperties.isProduction()` is true, so a misconfigured `AUTH_MODE=dev` in production fails closed twice, not once. CLAUDE.md's conceptual `public`/`embedded` deployment modes are not distinct values `DeploymentProperties` currently recognizes — only `personal` and `production` exist in code today.

## Endpoint Matrix

Role column reflects `CANDIDATE` (default, all authenticated users) vs `ADMIN` (manually promoted, gated by explicit in-controller checks on the two admin-only rows below).

| Endpoint | Method | Auth Required | CANDIDATE (all authenticated users) | ADMIN |
|---|---|---|---|---|
| `/api/v1/auth/me` | GET | Public route, but 401 thrown programmatically if principal is null | Own profile only (JWT claims) | N/A — not implemented |
| `/api/v1/auth/logout` | POST | No (clears cookie regardless) | Clears own session cookie | N/A |
| `/api/v1/me` | DELETE | Yes | Permanently deletes own account + all associated data (GDPR right to erasure) — irreversible | Same as CANDIDATE, no admin override |
| `/api/v1/admin/stats` | GET | Yes | **403 `FORBIDDEN_ADMIN_ONLY`** — `requireAdmin()` check in `AdminController` | Platform usage/health stats |
| `/api/v1/admin/system-feedback/{id}/applied` | PATCH | Yes | **403 `FORBIDDEN_ADMIN_ONLY`** — `requireAdmin()` check in `SystemFeedbackController` | Toggle whether a feedback submission has been applied |
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
| `/api/v1/assessment/report/{userId}` | GET | Yes | Own report only — `principal.id()` compared against path `userId`; mismatch → 403 `ASSESSMENT_ACCESS_DENIED` (fixed, see Known Gaps) | N/A |
| `/api/v1/curriculum/{userId}` | GET | Yes | Own plan only — `principal.id()` compared against path `userId`; mismatch → 403 `CURRICULUM_ACCESS_DENIED` (fixed, see Known Gaps) | N/A |
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

1. ~~**`{userId}` path parameters are not verified against the authenticated principal.**~~ **FIXED.** `/api/v1/assessment/report/{userId}` and `/api/v1/curriculum/{userId}` previously accepted `userId` as a raw path variable with no check against the authenticated principal — an IDOR (CWE-639): any authenticated user could substitute another user's UUID and read their assessment report or curriculum plan. Both controllers now derive the requesting identity from `@AuthenticationPrincipal AuthenticatedUser` and compare it against the path `userId` before calling the service layer; a mismatch throws `AssessmentException(ASSESSMENT_ACCESS_DENIED, 403)` / `CurriculumException(CURRICULUM_ACCESS_DENIED, 403)` and the underlying service is never invoked (verified via `verifyNoInteractions` in the regression tests). See `docs/decisions/` for context — this was surfaced during the authz-matrix authoring pass itself (writing the matrix required reading the actual controller source, not just describing intended behavior), fixed the same session. Covered by `AssessmentControllerTest.report_otherUsersReport_returns403` and `CurriculumControllerTest.generateCurriculum_otherUsersCurriculum_returns403`.
2. ~~**No `ADMIN` role exists.**~~ **PARTIALLY FIXED.** `Role.ADMIN` now exists (`users.role`, `V9__add_user_role.sql`) and gates two endpoints (`AdminController`, `SystemFeedbackController`). Still no framework-level enforcement: no `@EnableMethodSecurity`, no `@PreAuthorize`/`hasRole()`, no `SecurityConfig` matcher rule keyed on role — each new admin endpoint must remember to call `requireAdmin()` itself, which is a repeat-yourself risk (miss the call once and the endpoint is open to any authenticated user) that framework-level enforcement would eliminate.
3. **API documentation is `permitAll` in all environments** (no environment-based restriction currently implemented despite the comment noting it should be prod-restricted).

See also: [[mentorship/weak-areas]], [[hld]]
