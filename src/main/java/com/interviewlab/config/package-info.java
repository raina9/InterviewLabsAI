/**
 * Application configuration beans.
 *
 * Classes in this package (to be built per checkpoint):
 *   SecurityConfig         — Spring Security filter chain, OAuth2 login, JWT resource server
 *   CorsConfig             — CORS settings from app.cors.* properties
 *   OpenApiConfig          — OpenAPI metadata (title, description, contact, security scheme)
 *   RestClientConfig       — Spring RestClient bean for Gemini AI HTTP calls
 *   KafkaConfig            — Producer/consumer factory, retry topic, DLQ wiring
 *   RateLimitConfig        — Rate limiter bean (in-memory V1, Redis V2)
 *   JwtConfig              — JWT secret key materialisation from app.jwt.secret
 *   WebMvcConfig           — CORS registration, additional MVC customisations
 *
 * Configuration principle: all values injected from application.yml via @ConfigurationProperties
 * or @Value. No hardcoded values in any config class.
 */
package com.interviewlab.config;
