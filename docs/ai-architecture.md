# AI Architecture

## Agents

### InterviewAgent
**Responsibility:** question generation and interview flow ONLY. Does not evaluate answers.

- `initSession(userId, sessionId)` — builds context via `AgentToolChain`, generates the first question, persists it as an `INTERVIEWER` message
- `nextTurn(userId, sessionId, candidateAnswer, voiceUsed)` — persists the candidate's answer, rebuilds context, generates the next follow-up question via `InterviewAgentPromptBuilder.buildFollowUpPrompt()`, returns `InterviewTurnResult(agentResponse, shouldMoveToNextQuestion, questionNumber, candidateMessageId)`
- V1 behavior: `shouldMoveToNextQuestion` is always `false` — the agent always returns a follow-up rather than deciding to advance; question progression logic is a documented V2 gap, not yet implemented
- Uses `AiProperties.OptionsConfig.questionsTemperature/questionsMaxTokens` (temperature 0.7, higher token budget — question generation is creative, not constrained)

### MentorAgent
**Responsibility:** answer evaluation ONLY. Does not generate questions or manage session flow.

- `analyze(sessionId, messageId, question, candidateAnswer)` — builds context, calls `AIProviderStrategy.generateJson()` via `MentorAgentPromptBuilder.buildFeedbackPrompt()`, parses the JSON response into a `MentorFeedback` record: `feedbackGood`, `feedbackImprove`, `refinedAnswer`, `modelAnswer`, `psychologyNote`, `score`
- JSON parsing is defensive: finds the first `{` and last `}` in the raw response (LLMs frequently wrap JSON in prose or markdown fences) rather than trusting the response to be pure JSON; a parse failure throws `AIProviderException(AI_RESPONSE_PARSE_FAILED)` rather than silently returning a default/empty feedback object
- Uses `AiProperties.OptionsConfig.feedbackTemperature/feedbackMaxTokens` (temperature 0.3 — feedback should be precise and consistent, not creative)

**Boundary enforcement:** the two agents never call each other directly. `InterviewController`/`InterviewService` orchestrates both — `InterviewAgent.nextTurn()` then `MentorAgent.analyze()` — per the mentor loop below. Neither agent holds a reference to the other.

## Tool Chain — Chain of Responsibility

`AgentToolChain.execute(AgentContext)` runs every `AgentTool` bean in `@Order` sequence and returns `Map<toolName, resultString>`. A failing tool logs a warning and contributes an empty string — **the chain never aborts on a single tool failure**, so one broken data source degrades context quality rather than breaking the interview.

| Order | Tool | Name Key | Data Source | Behavior |
|---|---|---|---|---|
| 1 | `ResumeContextTool` | `resume` | `user_profiles.resume_text` | Returns resume text, or empty if blank/absent |
| 2 | `SessionHistoryTool` | `history` | `messages` table | Returns the last N messages, windowed by `app.agent.history-window-size` (`AgentProperties.historyWindowSize()`) — a sliding window, not the full session history |
| 3 | `ProficiencyTool` | `proficiency` | `proficiency` table | Returns `topic:score` lines for the user across all tracked topics |
| 4 | `QnAReferenceTool` | `qna` | — | **Stub — always returns empty string.** Web/external Q&A source integration is parked; the seam exists (`@Order(4)` slot in the chain) but nothing is wired to it yet |
| 5 | `UserPromptTool` | `userPrompt` | `user_profiles.custom_prompt` | Returns the user's saved custom instruction, or empty if none set |

`AgentContext(userId, sessionId, messageId, candidateAnswer)` is the immutable record threaded through every tool call. `messageId`/`candidateAnswer` are nullable — populated only during mentor-loop cycles (answer evaluation), null during question-generation-only calls.

## AI Provider — Strategy Pattern

`AiProviderStrategy` interface (`generate()`, `generateJson()`, `providerName()`) is implemented by four providers, selected by `AIProviderFactory` via an exhaustive `switch` over the `AiProvider` enum (`OLLAMA`, `GEMINI`, `CLAUDE`, `OPENAI`):

| Provider | Status | Mechanism |
|---|---|---|
| `OllamaProvider` | **Active (default)** | Spring AI Ollama starter, local `llama3.2`, zero cost — [[decisions/ADR-001-ollama-over-gemini]] |
| `GeminiProvider` | Wired, parked | Direct `RestClient` call to Google's `generateContent` REST API (no Spring AI dependency) — fully switchable via `AI_PROVIDER=gemini`, no code change |
| `ClaudeProvider` | Parked | `@ConditionalOnProperty(app.ai.provider=claude)` — bean not created unless configured; factory receives it as `Optional<ClaudeProvider>` so startup never fails on a missing bean |
| `OpenAIProvider` | Parked | Same pattern as Claude |

Switch mechanism: `AI_PROVIDER` env var (`app.ai.provider`, `application.yml`). `DEFAULT_AI_PROVIDER` (`app.ai.default-provider`) is the value written into `user_profiles.preferred_ai_provider` for new profiles — allowing (in principle) a future per-user provider preference, though nothing currently reads that column back to override the active provider per-request; `AIProviderFactory.getDefaultProvider()` always resolves from `AiProperties.defaultProvider()`, not from the user profile. Documented gap, not yet closed.

`AIProviderFactory.getProvider()` never returns a raw provider — every provider is wrapped in `QueuedAiProviderStrategy` (Decorator pattern) before being handed back, so `AIRequestQueue`/`AiBudgetGuard` (below) sit transparently in front of Ollama/Gemini/Claude/OpenAI without any provider implementation knowing about either concern. Full detail: [[lld/ai-provider-abstraction]].

Full pattern detail: [[decisions/ADR-004-conditional-on-property-pattern]].

## Context Management — Sliding Window + Token Budget

- **Sliding window:** `SessionHistoryTool` truncates message history to the last `app.agent.history-window-size` messages before it enters the prompt — bounds prompt size growth as a session gets longer, at the cost of the model losing visibility into earlier turns once the window slides past them
- **Token budget:** every AI call path has its own `(temperature, maxTokens)` pair, externalized to `application.yml` under `app.ai.*` (`AiProperties`) — never hardcoded in agent code:
  - General options (`options.*`): interview Q&A defaults
  - `feedback.*`: mentor loop (low temperature — precision over creativity)
  - `questions.*`: interview question generation (higher temperature — creativity over precision)
  - `quiz.*`, `code.*`, `curriculum.*`, `drill.*`: domain-specific budgets for their respective AI call sites, each tuned independently because their token needs (batch MCQ generation vs. single code review vs. Socratic dialogue) differ significantly
- **Request timeout:** `AI_REQUEST_TIMEOUT_SECONDS` (`app.ai.request-timeout-seconds`), enforced at the `RestClient` level in `WebMvcConfig` — covers Ollama's cold-start latency (30-40s on first request after idle) without leaving a hung request unbounded

## Cost & Concurrency Control — AIRequestQueue + AiBudgetGuard

Two independent chokepoints sit in front of every outbound AI provider call, added in V1.6 ([[decisions/ADR-011-ai-request-queue]]):

- **`AIRequestQueue`** — a fair (`fair=true`, FIFO) `Semaphore` bounding concurrent in-flight AI calls to `app.ai.queue.max-concurrent`. A caller that can't acquire a permit within `app.ai.queue.timeout-seconds` fails fast with `AI_BUSY` (429) rather than piling up unbounded threads against a slow/local Ollama instance. Chosen over reactive backpressure because it fits the existing MVC + virtual-threads stack without introducing a second concurrency model.
- **`AiBudgetGuard`** — a global daily kill switch, independent of per-user rate limiting: increments a `SessionStore`-backed counter (`ai:budget:{date}`, 25h TTL) on every call and rejects with `AI_BUDGET_EXHAUSTED` (503) once `app.ai.queue.daily-global-limit` is hit. Protects against a runaway bill from a bug (infinite retry loop, misbehaving agent) that stays under the concurrency cap but still accumulates unbounded call volume over a day. The counter only increments *after* a request clears `AIRequestQueue`, so a request rejected as `AI_BUSY` never consumes budget it didn't spend. `todaysCallCount()` is read-only and feeds the admin stats endpoint (`AdminController`).

Both are configured via `AiQueueProperties` (`app.ai.queue.*`) — `maxConcurrent`, `timeoutSeconds`, `dailyGlobalLimit` — never hardcoded.

## Guardrails — Gap

**No content-level guardrails are implemented.** No prompt-injection defense, no output content filtering, no PII redaction on resume/answer text before it's sent to a provider. This is an explicit, documented gap — not an oversight to be discovered later. Cost/concurrency guardrails (`AIRequestQueue`, `AiBudgetGuard` above) and per-user rate limiting (fixed-window, [[decisions/ADR-010-fixed-window-ratelimit]]) are implemented; content-level guardrails are not. For a public/embedded deployment mode (see CLAUDE.md `DEPLOYMENT MODES`), the content-level gap needs to be closed before launch; for `personal` mode (single user, relaxed limits), it's an acceptable V1 gap.

## Future — Roadmap

| Item | Status | Trigger to Unpark |
|---|---|---|
| pgvector + embeddings (`nomic-embed-text`) | Not implemented | Explicit workspace-wide AI Engineering track milestone (VishvakarmaAI Phase 3) |
| RAG (retrieval-augmented context, e.g. real Q&A bank via `QnAReferenceTool`) | Stub exists, not wired | Core loop stable + pgvector available |
| MCP (Model Context Protocol) | Not evaluated | Explicit owner instruction |
| A2A (agent-to-agent protocol) | Not evaluated | Explicit owner instruction — likely only relevant if `InterviewAgent`/`MentorAgent` need to coordinate beyond the current controller-orchestrated sequence |
| `CloudAgentOrchestrator` | Seam ready (`AgentOrchestrator` interface, `@ConditionalOnProperty(app.agent.orchestration-mode=cloud)`), not built | Multi-instance deploy or agent execution needs to scale independently of the rest of the monolith |

See also: [[mastery-tracker]], [[mentorship/weak-areas]]
