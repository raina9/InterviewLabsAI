# Auth Flow — LLD

Source-cited low-level design of the request path through the auth filter chain, where
`AuthProperties` binds, and how `@WebMvcTest` slices see it.

## Plain English

Every HTTP request to the API passes through Spring Security's filter chain before it
reaches a controller. Interview Lab runs in one of two modes, switched by a single
property (`app.auth.mode`, env var `AUTH_MODE`):

- **dev mode** (default): a request is authenticated by sending a fixed shared secret in
  an `X-Dev-Token` header. No Google account needed — good for local development.
- **oauth mode**: a request is authenticated by a JWT stored in an `httpOnly` cookie,
  issued after a real Google OAuth2 login.

Only one of the two filters is ever registered per running instance — the mode decision
is made once, at startup, in `SecurityConfig.securityFilterChain()`.

## Request Path

```
Request
  │
  ▼
SecurityContextHolderFilter (Spring Security default)
  │
  ▼
DevTokenFilter            ◄── dev mode only
  or
JwtAuthFilter              ◄── oauth mode only (+ oauth2Login for the callback itself)
  │
  ▼
UsernamePasswordAuthenticationFilter (Spring default, unused — no form login)
  │
  ▼
authorizeHttpRequests rules
  │
  ▼
Controller
```

**dev mode** — `DevTokenFilter.java`
1. `shouldNotFilter()` returns `true` (skips) if `authProperties.mode()` is `"oauth"` — belt
   and suspenders, since dev mode only registers this filter to begin with.
2. No `X-Dev-Token` header → continues unauthenticated; Spring Security's
   `authorizeHttpRequests` rules decide whether the endpoint requires auth.
3. Header present but doesn't match `authProperties.devToken()` → `401` with
   `DEV_TOKEN_INVALID`, chain stops.
4. Header matches → builds an `AuthenticatedUser` from `authProperties.devUserId()`,
   `devUserEmail()`, `devUserName()` (all fixed config values, not looked up from the DB)
   and sets it on `SecurityContextHolder`.

**oauth mode** — `JwtAuthFilter.java` + `OAuth2SuccessHandler.java`
1. Google OAuth2 login (`oauth2Login()`, Spring Security's built-in flow) runs first for
   the `/login/oauth2/**` callback path. On success, `OAuth2SuccessHandler` upserts the
   user (`UserService.findOrCreate`), signs a JWT (`JwtService.signToken`), and sets it as
   an `httpOnly`, `SameSite=Lax` cookie named `jwt` — the cookie name constant
   (`JwtAuthFilter.JWT_COOKIE_NAME`) is shared between the two classes so they never drift.
2. On every subsequent request, `JwtAuthFilter` reads the `jwt` cookie, verifies it via
   `JwtService.verifyToken`, and sets `SecurityContextHolder` from the claims. An invalid
   or expired token clears the cookie and continues unauthenticated (never throws) —
   protected endpoints are still gated by `authorizeHttpRequests`, not by this filter.

## Filter Bean Registration (2026-07-02 fix — G13 follow-on)

"Only one of the two filters is ever registered" (above) was, until this fix, only true
for Spring Security's own `SecurityFilterChain` — not for the servlet container. Before
this session, `SecurityConfig.jwtAuthFilter()` and `.devTokenFilter()` were both plain
`@Bean` methods. Spring Boot auto-registers *any* bean implementing `jakarta.servlet.Filter`
(both extend `OncePerRequestFilter`) as a generic servlet-container filter — a mechanism
completely separate from, and unaware of, the conditional `addFilterBefore(...)` call
inside `securityFilterChain()`. The practical effect: `JwtAuthFilter` ran on **every**
request in the running instance regardless of `AUTH_MODE`, in addition to whichever filter
Spring Security's chain actually selected for that mode.

This was invisible until `AuthControllerTest`'s `logout_clearsCookieAndReturns204` test
(which deliberately attaches a `jwt` cookie to exercise cookie-clearing) started actually
reaching the filter chain (G9's fix let the test class bootstrap at all) — the raw
auto-registered `JwtAuthFilter` picked up that cookie in **dev mode**, called
`jwtService.verifyToken()`, and NPE'd (`JwtAuthFilter.java:57`, `principal.email()`) because
the token wasn't a real signed JWT. In production this is a live bug: any dev-mode request
carrying a stale or forged `jwt` cookie hits `JwtAuthFilter` even though dev mode is
supposed to run `DevTokenFilter` only.

**Fix**: removed `@Bean` from both `jwtAuthFilter()` and `devTokenFilter()` — they are now
plain private factory methods, only reachable through the single conditional
`addFilterBefore(...)` call in `securityFilterChain()`. Neither is a managed Spring bean
anymore, so Spring Boot's generic filter auto-registration no longer sees them.

**Rule for future filter beans**: a filter meant to run only inside
`HttpSecurity.addFilterBefore/After/At(...)` must **not** also be a `@Bean` — declare it as
a plain (non-`@Bean`) factory method, or Spring Boot's servlet-filter auto-configuration
will register it a second time, unconditionally, on every request.

## Where `AuthProperties` Binds

`AuthProperties.java` (`src/main/java/com/interviewlab/auth/AuthProperties.java`) is a
`record` annotated `@ConfigurationProperties(prefix = "app.auth")`, bound from the
`app.auth.*` block in `application.yml` (lines 170–188):

```yaml
app:
  auth:
    mode: ${AUTH_MODE:dev}
    auto-detect: ${AUTH_AUTO_DETECT:true}
    dev-token: ${DEV_TOKEN:dev-secret}
    dev-user-id: ${DEV_USER_ID:00000000-0000-0000-0000-000000000001}
    dev-user-email: ${DEV_USER_EMAIL:dev@interviewlab.local}
    dev-user-name: ${DEV_USER_NAME:InterviewLab Dev}
```

`SecurityConfig` (`src/main/java/com/interviewlab/config/SecurityConfig.java`) is the
only consumer: it constructor-injects `AuthProperties` and calls `.mode()` and
`.autoDetect()` directly inside the `securityFilterChain()` `@Bean` method to decide which
filter to register (`useOAuth` at line 91), and passes the whole record into
`DevTokenFilter`'s constructor.

In the full application, `AuthProperties` is registered as a Spring bean by
`@ConfigurationPropertiesScan` on `InterviewLabApplication` (classpath scan at startup —
no per-class annotation needed, by design, per that class's own Javadoc).

## How Test Slices See It (2026-07-02 fix)

`@WebMvcTest` starts a narrow slice of the application context — controllers, `@Configuration`
classes explicitly `@Import`ed by the test, and Spring MVC infrastructure. It does **not**
run `@ConfigurationPropertiesScan`, because that annotation lives on
`InterviewLabApplication`, the `@SpringBootApplication` class, which `@WebMvcTest` never
loads. This is standard Spring Boot 4.1 test-slice behavior, not a bug in the annotation
itself.

Every one of Interview Lab's 11 controller test classes does
`@Import(SecurityConfig.class)` (to get real security behavior in the slice) and
`@MockitoBean`-mocks `JwtService` and `OAuth2SuccessHandler` (the two other
`SecurityConfig` dependencies that don't need real config to construct). Nothing mocked
`AuthProperties` — and because `SecurityConfig.securityFilterChain()` calls
`authProperties.mode()` as a real method call (not just a field reference) while building
the filter chain bean, the bean has to actually exist and be usable, not just be present as
a type on the classpath.

Before the fix: `NoSuchBeanDefinitionException` for `AuthProperties` on every one of those
11 classes — the slice tried to construct `SecurityConfig`, which needs `AuthProperties`,
which was never registered because `@ConfigurationPropertiesScan` never ran in the slice.

**Fix**: `SecurityConfig` now carries
`@EnableConfigurationProperties(AuthProperties.class)` directly. Since every affected test
already `@Import(SecurityConfig.class)`, this registers `AuthProperties` as a side effect
of that existing import — no per-test `@MockBean`, no test-only property overrides, no
changes to any of the 11 test files. `application-test.yml` has no `app.auth.*` overrides,
so the slice binds the same defaults as the real app (`mode=dev`, `auto-detect=true`,
`dev-token=dev-secret`, etc.) via `application.yml`'s env-var-with-default syntax.

`@EnableConfigurationProperties` is safe to combine with `@ConfigurationPropertiesScan` on
the same class in the full application context — Spring's
`ConfigurationPropertiesBeanRegistrar` checks for an existing bean definition of the same
name before registering and skips if one is already present, so there's no duplicate-bean
conflict when the full `InterviewLabApplication` context starts.

**Rule for future `@ConfigurationProperties` classes**: if a `@Configuration` class that a
test slice `@Import`s constructor-injects a `@ConfigurationProperties` type, that
`@Configuration` class needs its own `@EnableConfigurationProperties(...)` — relying on
`@ConfigurationPropertiesScan` from the `@SpringBootApplication` class alone will pass in
the full app context and fail in every test slice.

## Interview Talking Points

- Why constructor-call-time binding matters: a `@MockitoBean`/mock of `AuthProperties`
  would have "fixed" the `NoSuchBeanDefinitionException` too, but the record's real
  bound values (`mode=dev`, `auto-detect=true`) are what make `SecurityConfig` build the
  same filter chain shape (`DevTokenFilter` registered) as production. A mock returning
  `null`/`false` for everything happens to also route to the dev-mode branch here, but
  that's incidental — the intent is to exercise real config binding, not a stand-in.
- Why `@EnableConfigurationProperties` on `SecurityConfig` beats a shared
  `@TestConfiguration`: every test slice already imports `SecurityConfig`, so the fix
  piggybacks on an import that already exists everywhere, rather than adding a new import
  line to 11 files.
- `@ConfigurationPropertiesScan` vs `@EnableConfigurationProperties` — the former is a
  classpath scan (convenient, but only runs when the class carrying it is part of the
  loaded context); the latter is an explicit, per-class registration that works regardless
  of which slice of the application is under test.
