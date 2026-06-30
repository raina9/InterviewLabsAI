package com.interviewlab.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewlab.ai.AIOptions;
import com.interviewlab.ai.AIProviderFactory;
import com.interviewlab.auth.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class CodeChallengeService {

    private static final AIOptions CHALLENGE_OPTIONS = new AIOptions(0.5f, 1800, false);
    private static final AIOptions REVIEW_OPTIONS    = new AIOptions(0.3f, 1200, false);

    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
        "java",       62,
        "python",     71,
        "javascript", 63
    );

    private final AIProviderFactory aiProviderFactory;
    private final Judge0Properties  judge0Properties;
    private final ObjectMapper      objectMapper;
    private final RestClient.Builder restClientBuilder;

    private final Map<UUID, CodeChallenge> challenges = new ConcurrentHashMap<>();

    public CodeChallenge generateChallenge(CodeChallengeRequest request) {
        String prompt = buildChallengePrompt(request.topic(), request.difficulty());
        String raw    = aiProviderFactory.getDefaultProvider().generateJson(prompt, CHALLENGE_OPTIONS);
        try {
            String json  = extractJson(raw);
            CodeChallenge partial = objectMapper.readValue(json, CodeChallenge.class);
            CodeChallenge challenge = new CodeChallenge(
                UUID.randomUUID(),
                partial.title(),
                partial.description(),
                partial.starterCode(),
                partial.testCases(),
                partial.constraints()
            );
            challenges.put(challenge.id(), challenge);
            log.info("Code challenge generated: id={} topic={}", challenge.id(), request.topic());
            return challenge;
        } catch (Exception e) {
            log.error("Failed to parse code challenge for topic={}: {}", request.topic(), e.getMessage());
            throw new CodeChallengeException(
                ErrorCode.CODE_CHALLENGE_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate code challenge. Please retry your request."
            );
        }
    }

    public CodeSubmitResponse submitSolution(CodeSubmitRequest request) {
        CodeChallenge challenge = challenges.get(request.challengeId());
        if (challenge == null) {
            throw new CodeChallengeException(
                ErrorCode.CODE_CHALLENGE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Code challenge " + request.challengeId() + " not found. Generate a new challenge."
            );
        }

        String executionResult;
        boolean passed;

        if (judge0Properties.isConfigured()) {
            Judge0SubmissionResult result = executeViaJudge0(request.code(), request.language());
            passed          = result.accepted();
            executionResult = formatExecutionResult(result);
        } else {
            // Judge0 not configured — AI code review as fallback
            log.info("Judge0 not configured — using AI code review for challengeId={}", request.challengeId());
            executionResult = "Execution environment not configured. AI code review applied.";
            passed          = false; // determined by AI review below
        }

        CodeSubmitResponse aiReview = getAiCodeReview(challenge, request.code(), request.language(), executionResult);
        if (!judge0Properties.isConfigured()) {
            return aiReview;
        }
        return new CodeSubmitResponse(passed, aiReview.feedback(), aiReview.refinedCode(), aiReview.explanation(), executionResult);
    }

    public String getHint(UUID challengeId) {
        CodeChallenge challenge = challenges.get(challengeId);
        if (challenge == null) {
            throw new CodeChallengeException(
                ErrorCode.CODE_CHALLENGE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Code challenge " + challengeId + " not found."
            );
        }
        String prompt = """
            You are a coding mentor. Give a helpful hint (not the full solution) for this challenge:
            Title: %s
            Description: %s
            Constraints: %s
            Hint should guide the approach without revealing the answer. 2-3 sentences max.
            """.formatted(challenge.title(), challenge.description(), challenge.constraints());
        return aiProviderFactory.getDefaultProvider().generate(prompt, AIOptions.defaults());
    }

    private Judge0SubmissionResult executeViaJudge0(String code, String language) {
        int languageId = LANGUAGE_IDS.getOrDefault(language.toLowerCase(), 62);
        try {
            Map<String, Object> body = Map.of(
                "source_code", code,
                "language_id", languageId,
                "stdin",       ""
            );
            RestClient client = restClientBuilder.build();
            return client.post()
                .uri(judge0Properties.url() + "/submissions?base64_encoded=false&wait=true")
                .header("X-RapidAPI-Key", judge0Properties.apiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Judge0SubmissionResult.class);
        } catch (Exception e) {
            log.warn("Judge0 execution failed — falling back to AI review: {}", e.getMessage());
            return new Judge0SubmissionResult(
                new Judge0SubmissionResult.Judge0Status(0, "Execution failed"),
                null, e.getMessage(), null, null
            );
        }
    }

    private CodeSubmitResponse getAiCodeReview(CodeChallenge challenge, String code, String language, String executionResult) {
        String prompt = """
            You are a senior software engineer reviewing a coding submission.
            Challenge: %s
            Description: %s
            Language: %s
            Submitted code:
            ```
            %s
            ```
            Execution result: %s
            Return ONLY valid JSON:
            {
              "passed": true,
              "feedback": "...",
              "refinedCode": "...",
              "explanation": "..."
            }
            Be specific. passed=true only if the logic is correct and handles all cases.
            """.formatted(challenge.title(), challenge.description(), language, code, executionResult);
        String raw = aiProviderFactory.getDefaultProvider().generateJson(prompt, REVIEW_OPTIONS);
        try {
            String json = extractJson(raw);
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            return new CodeSubmitResponse(
                Boolean.TRUE.equals(parsed.get("passed")),
                (String) parsed.get("feedback"),
                (String) parsed.get("refinedCode"),
                (String) parsed.get("explanation"),
                executionResult
            );
        } catch (Exception e) {
            log.error("Failed to parse AI code review: {}", e.getMessage());
            throw new CodeChallengeException(
                ErrorCode.CODE_EVALUATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to evaluate code submission. Please retry your request."
            );
        }
    }

    private String buildChallengePrompt(String topic, String difficulty) {
        return """
            You are a coding challenge generator for software engineering interviews.
            Generate a %s difficulty coding challenge about "%s".
            Return ONLY valid JSON — no markdown, no explanation:
            {
              "title": "...",
              "description": "...",
              "starterCode": {
                "java": "// Java starter code here",
                "python": "# Python starter code here",
                "javascript": "// JavaScript starter code here"
              },
              "testCases": ["Input: ... Expected: ...", "Input: ... Expected: ..."],
              "constraints": ["0 <= n <= 10^5", "1 <= value <= 1000"]
            }
            Rules: description must be detailed and unambiguous. Include 2-3 test cases and 2-4 constraints.
            """.formatted(difficulty, topic);
    }

    private String formatExecutionResult(Judge0SubmissionResult result) {
        if (result == null) return "No result";
        StringBuilder sb = new StringBuilder();
        if (result.status() != null) sb.append("Status: ").append(result.status().description());
        if (result.stdout() != null && !result.stdout().isBlank()) sb.append(" | Output: ").append(result.stdout().trim());
        if (result.stderr() != null && !result.stderr().isBlank()) sb.append(" | Error: ").append(result.stderr().trim());
        if (result.time()  != null) sb.append(" | Time: ").append(result.time()).append("s");
        return sb.toString();
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new CodeChallengeException(
                ErrorCode.CODE_CHALLENGE_GENERATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to generate code challenge. Please retry your request."
            );
        }
        return raw.substring(start, end + 1);
    }
}
