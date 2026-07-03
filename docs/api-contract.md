# InterviewLab — API Contract

Base path: `/api/v1`  
Auth: all endpoints require authentication except `/auth/**`, `/actuator/health`, and static assets.  
Dev auth: `X-Dev-Token: dev-secret` header.  
OAuth auth: `Authorization: Bearer <jwt>` cookie set by `/auth/oauth2/callback`.  
Error envelope: `{ "errorCode": "...", "message": "...", "status": 4xx|5xx }`

---

## Auth

### GET /api/v1/auth/me
Returns the authenticated user.  
Response 200: `{ "id": UUID, "email": "...", "name": "...", "picture": "..." }`  
Response 401: AUTH_TOKEN_MISSING

### GET /api/v1/auth/oauth2/callback
OAuth2 redirect handler. Sets JWT cookie and redirects to FRONTEND_REDIRECT_URL.

### GET /api/v1/auth/logout
Clears the JWT cookie.

---

## Sessions

### POST /api/v1/sessions
Create a new interview session.  
Request: `{ "interviewType": "TECHNICAL|HR|SYSTEM_DESIGN|BEHAVIOURAL", "targetRole": "...", "jdText": "...", "difficulty": "EASY|MEDIUM|HARD", "targetCompany": "...", "topicFocus": "..." }`  
Response 201: Session object with `{ "id", "userId", "interviewType", "targetRole", "status": "ACTIVE", "createdAt" }`  
Response 400: VALIDATION_FAILED (missing required fields)

### GET /api/v1/sessions
List all sessions for the authenticated user.  
Response 200: Array of Session summary objects.

### GET /api/v1/sessions/{id}
Get a session by ID.  
Response 200: Session object.  
Response 403: SESSION_ACCESS_DENIED (session belongs to a different user)  
Response 404: SESSION_NOT_FOUND

### POST /api/v1/sessions/{id}/complete
Transition session to COMPLETED.  
Response 200: Updated Session object.

### POST /api/v1/sessions/{id}/abandon
Transition session to ABANDONED.  
Response 200: Updated Session object.

### GET /api/v1/sessions/{id}/messages
Retrieve the full message history for a session.  
Response 200: Array of `{ "id", "role": "INTERVIEWER|CANDIDATE", "content", "sequence", "voiceUsed", "createdAt" }`

---

## Interview

### POST /api/v1/interview/start
Start an interview — generates the first question.  
Request: `{ "sessionId": UUID }`  
Response 200: `{ "sessionId": UUID, "firstQuestion": "...", "totalQuestions": N }`  
Response 409: INTERVIEW_ALREADY_STARTED  
Response 409: INTERVIEW_SESSION_NOT_ACTIVE

### POST /api/v1/interview/{sessionId}/respond
Submit a candidate answer — returns next question + mentor feedback + optional psychology nudge.  
Request: `{ "answer": "...", "voiceUsed": false }`  
Response 200:
```json
{
  "agentResponse": "...",
  "sessionComplete": false,
  "mentorFeedback": {
    "feedbackGood": "...",
    "feedbackImprove": "...",
    "refinedAnswer": "...",
    "modelAnswer": "...",
    "psychologyNote": "...",
    "score": 7
  },
  "psychologyNudge": {
    "pattern": "IMPROVING",
    "nudge": "...",
    "actionableAdvice": "..."
  }
}
```
`psychologyNudge` is `null` unless the current answer is the 3rd, 6th, 9th... scored answer.
`sessionComplete` is `true` once the answered-question count reaches the session's
`totalQuestions` — the session is transitioned to `COMPLETED` server-side in the same call (same
transition + `session.completed` event as `POST /api/v1/sessions/{id}/complete`), so the
frontend does not need to call Finish separately when this fires.

### GET /api/v1/interview/{sessionId}/feedback
Retrieve all scored feedback for a session.  
Response 200: Array of MentorFeedback objects (same structure as above).

---

## Profile

### GET /api/v1/profile
Get the authenticated user's profile.  
Response 200: `{ "userId", "experienceYears", "currentPosition", "techStack", "resumeText", "customPrompt", "preferredAiProvider" }`  
Response 404: PROFILE_NOT_FOUND

### PUT /api/v1/profile
Update profile fields.  
Request: `{ "experienceYears": N, "currentPosition": "...", "techStack": ["Java","Spring"] }`  
Response 200: Updated ProfileResponse.

### PUT /api/v1/profile/resume
Update resume text.  
Request: `{ "resumeText": "..." }`  
Response 200: Updated ProfileResponse.

### PUT /api/v1/profile/custom-prompt
Update the candidate's custom interview context prompt.  
Request: `{ "customPrompt": "..." }`  
Response 200: Updated ProfileResponse.

---

## Assessment

### POST /api/v1/assessment/start
Begin a self-assessment — returns topics derived from the user's tech stack.  
Response 200: `{ "topics": ["Java","Spring Boot",...], "instructions": "Rate each topic from 1 to 10" }`  
Response 404: ASSESSMENT_PROFILE_NOT_FOUND (profile not yet created)

### POST /api/v1/assessment/submit
Submit self-ratings. Upserts proficiency rows.  
Request: `{ "ratings": [{ "topic": "Java", "rating": 7 }, ...] }`  
Constraint: `ratings` not empty; each `rating` is 1–10.  
Response 200: (empty body)  
Response 400: VALIDATION_FAILED

### GET /api/v1/assessment/report/{userId}
Get a proficiency report for the given user.  
Response 200:
```json
{
  "topics": [{ "topic": "Java", "selfRating": 7, "level": "Senior", "recommendation": "..." }],
  "overallLevel": "Intermediate",
  "criticalGaps": ["Kafka"],
  "quickWins": ["Spring Boot"]
}
```
Response 404: ASSESSMENT_NOT_FOUND (no ratings submitted yet)

---

## Curriculum

### GET /api/v1/curriculum/{userId}
Generate a personalised learning curriculum from the proficiency report.  
Response 200:
```json
{
  "items": [{
    "topic": "Kafka",
    "priority": "HIGH",
    "whyThisMatters": "...",
    "estimatedDays": 14,
    "keyConceptsToCover": ["Producers", "Consumer groups", "Partitions"]
  }],
  "estimatedWeeks": 6,
  "focus": "Backend engineering and distributed systems"
}
```
Response 404: ASSESSMENT_NOT_FOUND (no assessment data; call /assessment/start first)  
Response 500: CURRICULUM_GENERATION_FAILED (AI parse error; retry)

---

## Quiz

### POST /api/v1/quiz/start
Start a new MCQ quiz.  
Request: `{ "topic": "Java", "difficulty": "easy|medium|hard", "questionCount": 5 }`  
Constraint: `questionCount` is 3–20.  
Response 200: `{ "sessionId": UUID, "topic", "difficulty", "totalQuestions", "currentIndex": 0, "score": 0, "currentQuestion": "...", "currentOptions": [...] }`  
Response 500: QUIZ_GENERATION_FAILED

### POST /api/v1/quiz/{sessionId}/answer
Submit an answer to the current question.  
Request: `{ "answer": "O(1)" }` (must match one of the options exactly, case-insensitive)  
Response 200:
```json
{
  "correct": true,
  "explanation": "...",
  "correctAnswer": "O(1)",
  "score": 1,
  "totalAnswered": 1,
  "sessionComplete": false,
  "nextQuestion": "...",
  "nextOptions": [...]
}
```
`nextQuestion` and `nextOptions` are `null` when `sessionComplete=true`.  
Response 404: QUIZ_SESSION_NOT_FOUND  
Response 409: QUIZ_ALREADY_COMPLETED

### GET /api/v1/quiz/{sessionId}/result
Retrieve the final quiz result. Removes the session from memory.  
Response 200: `{ "totalQuestions", "correctAnswers", "scorePercent", "topicBreakdown": { "Java": 4 } }`  
Response 409: QUIZ_NOT_YET_COMPLETE (all questions must be answered first)

---

## Topic Drill

### POST /api/v1/drill/start
Start a drill session.  
Request: `{ "topic": "Java Concurrency", "mode": "RAPID|DEEP" }`  
RAPID: pre-generates 10 quick Q&A questions; 1–2 sentence answers expected.  
DEEP: generates one Socratic question; each answer generates a deeper follow-up.  
Response 200: `{ "sessionId": UUID, "topic", "mode", "currentQuestion": "...", "questionsAnswered": 0, "complete": false }`  
Response 500: DRILL_GENERATION_FAILED

### POST /api/v1/drill/{sessionId}/next
Submit answer and receive the next question + feedback + score.  
Request: `{ "answer": "..." }`  
Response 200: `{ "question": "...", "questionNumber": 2, "sessionComplete": false, "feedback": "...", "previousScore": 7 }`  
`question` is `null` when `sessionComplete=true`.  
Response 404: DRILL_SESSION_NOT_FOUND  
Response 409: DRILL_ALREADY_COMPLETED  
Response 500: DRILL_GENERATION_FAILED (AI score or follow-up generation failed)

### GET /api/v1/drill/{sessionId}/summary
Get the session summary. Removes the session from memory.  
Response 200: `{ "topic", "mode", "questionsAnswered", "avgScore", "weakSpots": [...], "strongPoints": [...] }`  
weakSpots: questions with score < 5. strongPoints: questions with score ≥ 7.

---

## Code Challenge

### POST /api/v1/code/challenge
Generate a coding challenge.  
Request: `{ "topic": "Arrays", "difficulty": "easy|medium|hard" }`  
Response 200:
```json
{
  "id": UUID,
  "title": "Two Sum",
  "description": "...",
  "starterCode": { "java": "...", "python": "...", "javascript": "..." },
  "testCases": ["Input: [2,7,11,15] target=9 Expected: [0,1]"],
  "constraints": ["2 <= nums.length <= 10^4"]
}
```
Response 500: CODE_CHALLENGE_GENERATION_FAILED

### POST /api/v1/code/submit
Submit a solution. Evaluated via Judge0 if configured, otherwise AI code review.  
Request: `{ "challengeId": UUID, "code": "...", "language": "java|python|javascript" }`  
Response 200: `{ "passed": true, "feedback": "...", "refinedCode": "...", "explanation": "...", "executionResult": "..." }`  
Response 404: CODE_CHALLENGE_NOT_FOUND  
Response 500: CODE_EVALUATION_FAILED

### GET /api/v1/code/challenge/{id}/hint
Get a hint for the challenge — 2–3 sentences, no full solution.  
Response 200: String (plain text)  
Response 404: CODE_CHALLENGE_NOT_FOUND

---

## English Proficiency

### POST /api/v1/english/analyse
Analyse a spoken/written transcript for proficiency.  
Request: `{ "transcript": "...", "context": "Interview answer about system design" }`  
Response 200: `{ "fluencyScore": 7, "grammarScore": 8, "vocabularyScore": 6, "confidenceScore": 7, "fillerWordCount": 3, "feedback": "...", "suggestions": [...] }`  
Response 500: ENGLISH_ANALYSIS_FAILED

---

## Voice

### POST /api/v1/voice/transcript
Submit a speech-to-text transcript — delegates to the same turn-processing flow as
`POST /api/v1/interview/{sessionId}/respond`, with `voiceUsed` always `true`.  
Request: `{ "sessionId": UUID, "transcript": "..." }`  
Response 200: same shape as `POST /api/v1/interview/{sessionId}/respond` —
`{ "agentResponse", "sessionComplete", "mentorFeedback", "psychologyNudge" }`

---

## Webhooks (planned)

`session.completed` — fires when a session is marked COMPLETED.  
`answer.scored` — fires after each MentorAgent evaluation.

---

## Common Mistakes

**1. Calling /assessment/report before /assessment/submit**  
Error: ASSESSMENT_NOT_FOUND (404). Call `/assessment/start` → show topics → collect ratings → POST `/assessment/submit` first.

**2. Calling /quiz/{id}/result before answering all questions**  
Error: QUIZ_NOT_YET_COMPLETE (409). Submit all questions via `/{id}/answer` until `sessionComplete=true`, then call `/result`.

**3. Calling /drill/{id}/next after `sessionComplete=true`**  
Error: DRILL_ALREADY_COMPLETED (409). The session is done; call `/{id}/summary`.

**4. Submitting `answerCount > questionCount` in Quiz**  
Error: QUIZ_ALREADY_COMPLETED (409). The session is sealed when all questions are answered.

**5. Expecting psychologyNudge on every turn**  
`psychologyNudge` is null unless the current scored answer count is divisible by 3. Check for null before rendering.

**6. Expecting Judge0 execution when not configured**  
If JUDGE0_URL or JUDGE0_API_KEY is blank, `/code/submit` falls back to AI code review. `executionResult` will say "Execution environment not configured. AI code review applied."

**7. Using the wrong Auth header in production**  
Dev mode uses `X-Dev-Token` header. OAuth mode uses a JWT cookie set by `/auth/oauth2/callback`. Switching modes requires `AUTH_MODE=oauth` + Google credentials — not just changing headers.

**8. Calling /curriculum/{userId} with no assessment data**  
CurriculumService calls assessmentService.generateReport() internally. If no proficiency rows exist, it throws ASSESSMENT_NOT_FOUND (surfaced as 500 CURRICULUM_GENERATION_FAILED). Call `/assessment/submit` first.
