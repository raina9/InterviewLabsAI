/**
 * AI provider abstraction layer.
 *
 * Core types:
 * - AIProvider (sealed interface, Java 25) — enables exhaustive pattern matching across providers
 * - AIProviderFactory — creates the active provider from config (Factory pattern)
 * - GeminiAIProvider — V1 active implementation; calls Gemini REST API via RestClient
 * - AIRequest / AIResponse — Records (Java 25) for immutable provider I/O
 *
 * Provider strategy:
 *   Active:  Gemini (V1 — configured via app.ai.gemini.*)
 *   Parked:  Claude, OpenAI (plug-in ready — implement AIProvider sealed interface)
 *   Switch:  AI_PROVIDER env var → AIProviderFactory selects implementation at startup
 *
 * Token budgets are declared in application.yml per agent — never hardcoded here.
 * All LLM calls are I/O-bound — virtual threads handle concurrency automatically.
 */
package com.interviewlab.ai;
