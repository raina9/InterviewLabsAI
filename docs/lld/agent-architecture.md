# Agent Architecture — LLD

Source-cited low-level design of `InterviewAgent`/`MentorAgent`, the 5-tool Chain of
Responsibility that feeds them context, and the orchestration seam above the chain.

## Plain English

Every AI call in an interview turn needs the same kind of context — resume, recent
history, proficiency, etc. Rather than each agent method independently fetching that
context, a fixed pipeline of small "tool" classes each contribute one piece, and the
combined result is handed to the agent that actually talks to the LLM. The two agents
that consume this context are strictly separated: one only asks questions, the other
only evaluates answers. Neither calls the other directly — a service layer above both
sequences them per turn.

## Request Path — One Interview Turn

```
InterviewController.respond()
  │
  ▼
InterviewService.respond()
  │
  ├──► InterviewAgent.nextTurn(userId, sessionId, answer, voiceUsed)
  │      │
  │      ▼
  │    AgentOrchestrator.orchestrate(AgentContext)   ◄── seam (local vs cloud)
  │      │
  │      ▼
  │    LocalAgentOrchestrator (default) ──► AgentToolChain.execute(context)
  │      │                                    │
  │      │                                    ├─ 1. ResumeContextTool
  │      │                                    ├─ 2. SessionHistoryTool
  │      │                                    ├─ 3. ProficiencyTool
  │      │                                    ├─ 4. QnAReferenceTool   (stub)
  │      │                                    └─ 5. UserPromptTool
  │      │                                  Map<toolName, resultString>
  │      ▼
  │    InterviewAgentPromptBuilder.buildFollowUpPrompt(tool results)
  │      │
  │      ▼
  │    AiProviderStrategy.generate()  ──► InterviewTurnResult
  │
  └──► MentorAgent.analyze(sessionId, messageId, question, answer)
         │
         ▼
       (same AgentToolChain run again, fresh — tools are stateless)
         │
         ▼
       MentorAgentPromptBuilder.buildFeedbackPrompt(tool results)
         │
         ▼
       AiProviderStrategy.generateJson()  ──► MentorFeedback (parsed JSON)
```

`InterviewService.respond()` persists `AnswerFeedback`, publishes `answer.scored` via
`EventPublisher`, and computes a psychology nudge every 3rd answer — none of that lives
in either agent (source: `InterviewService.java:66-117`).

## Chain of Responsibility — `AgentToolChain`

`AgentToolChain.execute(AgentContext)` (`agent/AgentToolChain.java`) iterates every
`AgentTool` Spring bean in `@Order` sequence and builds a `LinkedHashMap<String, String>`
keyed by `tool.name()`. Each tool call is wrapped in its own try/catch — **a failing
tool logs a warning and contributes `""`, the loop never aborts**:

```java
for (AgentTool tool : tools) {
    try {
        results.put(tool.name(), tool.execute(context));
    } catch (Exception ex) {
        log.warn("AgentTool '{}' failed — contributing empty: {}", tool.name(), ex.getMessage());
        results.put(tool.name(), "");
    }
}
```

`AgentTool` (`agent/tools/AgentTool.java`) is a two-method interface: `name()`,
`execute(AgentContext)`. Tools are stateless — each `execute()` call fetches fresh data,
nothing is cached across turns. Full tool table (data source, behavior per tool):
[[ai-architecture]].

`AgentContext` (`agent/tools/AgentContext.java`) is the immutable record threaded through
every tool: `(userId, sessionId, messageId, candidateAnswer)`. `messageId`/`candidateAnswer`
are `null` during question-generation-only calls, populated only for mentor-loop cycles.

## Orchestration Seam — `AgentOrchestrator`

`AgentOrchestrator.orchestrate(AgentContext)` (`agent/orchestration/AgentOrchestrator.java`)
decouples "which tools to run" from "how to run them" — the same
[[swappable-backend-pattern]] used elsewhere in the codebase, keyed by
`AGENT_ORCHESTRATION_MODE`:

| Impl | Condition | Behavior |
|---|---|---|
| `LocalAgentOrchestrator` (default) | `app.agent.orchestration-mode=local` or unset (`matchIfMissing=true`) | Delegates straight to `AgentToolChain.execute()` — in-process, zero infra |
| `CloudAgentOrchestrator` (not built) | `app.agent.orchestration-mode=cloud` | Seam ready, no implementation exists yet — would delegate to a managed orchestration platform (LangChain, Vertex AI Agents, Bedrock Agents) |

`OrchestrationResult` wraps the same `Map<String, String>` the chain produces — the seam
adds no behavior beyond the routing decision today.

## Agent Boundary — InterviewAgent / MentorAgent

Enforced at the class level, not by a shared base class or interface — the two agents
simply never hold a reference to each other. `InterviewService` is the only caller of
both. Full responsibility breakdown, prompt builders, temperature/token config per agent:
[[ai-architecture]].

## Known Gaps

- `QnAReferenceTool` (`@Order(4)`) is a stub — always returns `""`. The seam exists; no
  external Q&A source is wired to it.
- `CloudAgentOrchestrator` has no implementation — the interface and conditional wiring
  exist, the class does not.

See also: [[ai-architecture]], [[swappable-backend-pattern]], [[interview-session-lifecycle]]
