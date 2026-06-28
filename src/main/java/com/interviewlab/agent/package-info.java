/**
 * Agentic core — interview flow and mentor loop agents.
 *
 * Agents (strict SRP — one job per agent, no overlap):
 *
 *   InterviewAgent  — question generation + session flow management ONLY.
 *                     Does NOT evaluate answers, give feedback, or generate model answers.
 *
 *   MentorAgent     — answer evaluation ONLY: feedback + refined answer + model answer
 *                     + psychology note. Does NOT generate questions or manage flow.
 *
 * AgentTools chain (Chain of Responsibility — execution order is deterministic):
 *   1. ResumeContextTool     — loads parsed resume for the current user
 *   2. SessionHistoryTool    — loads past session summaries (sliding window, bounded)
 *   3. ProficiencyTool       — loads English + domain proficiency scores
 *   4. QnAReferenceTool      — loads relevant Q&A from knowledge base
 *   5. UserPromptTool        — applies saved custom prompt from user profile
 *
 * Operating principles:
 * - Sliding window context: bounded by AI_CONTEXT_WINDOW env var — never unbounded
 * - Token budget: per-agent config in application.yml — never hardcoded
 * - Stateless tools: each tool run fetches fresh data from source
 * - Structured concurrency (Java 25): parallel tool fetches where order allows
 */
package com.interviewlab.agent;
