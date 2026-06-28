/**
 * Authentication and authorisation layer.
 *
 * Responsibilities:
 * - Google OAuth2 authorisation code flow (Spring Security OAuth2 Client)
 * - JWT issuance on successful OAuth2 login (access + refresh tokens)
 * - JWT validation filter (stateless — no server-side session)
 * - Security filter chain configuration
 * - Auth endpoints: /api/v1/auth/oauth2/callback, /api/v1/auth/refresh, /api/v1/auth/logout
 *
 * Patterns: Filter (JWT validation), Strategy (token provider — extensible for GitHub OAuth)
 * User identity: Google OIDC sub claim stored in users.google_sub
 */
package com.interviewlab.auth;
