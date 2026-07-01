# Workflow

How work actually gets done on InterviewLab — decisions, features, bugs, mentorship mode, token discipline, and git conventions. This is the process doc; `hld.md`/`api-contract.md`/`db-schema.md` are the artifacts it produces.

## How Decisions Are Made

```
Chat discussion (owner + Claude)
  → Options weighed explicitly: what's rejected, what's chosen, why
  → Decision recorded as an ADR in docs/decisions/ (see docs/decisions/ for the running log)
  → CLI/code reflects the decision — CLAUDE.md updated if the decision changes a standing rule
```

Every non-trivial architectural choice gets an ADR — not just the successful ones. A rejected option with a documented reason is as valuable as the chosen one, because it prevents re-litigating the same question months later without the original context. See `docs/decisions/ADR-001` through `ADR-008` for the actual record.

## How Features Are Built

```
Think  → What is this actually for? What's the input/output contract? What are the edge cases?
Design → Where does this live (which package)? What pattern applies (Strategy/Chain/Factory/etc)?
           Does it need a new swappable-backend seam?
Code   → Owner implements (Mode A: Claude builds the skeleton/mentor-project; owner studies,
           questions, and drives edits) — one step at a time, output reviewed before the next step
```

Never "just start coding." A vague requirement gets challenged before any code is written — CLAUDE.md `MENTOR BEHAVIOUR`: *"Challenge vague requirements — demand input/output/edge cases."*

## How Bugs Are Fixed

```
Reproduce   → Confirm the failure locally with the smallest possible repro
Root cause  → Never patch the symptom. See docs/decisions/ADR-006 and ADR-007 for two cases
                where root-causing required bytecode decompilation (javap) and network-layer
                debugging (netstat) respectively, rather than trial-and-error edits.
Fix         → Smallest change that addresses the actual cause
Test        → A fix without a test is not considered done — CLAUDE.md NON-NEGOTIABLES:
                "No feature without tests"
Document    → If the bug produced a reusable lesson, it becomes a NEVER DO THIS rule in
                CLAUDE.md (see the 8 standing rules currently in that section) and/or an entry
                in docs/mentorship/mistakes-and-fixes.md
```

## Mentorship Mode Rules

- **Mode A — Mentor-built (default from Phase 1 onward):** Claude builds, owner studies and questions. This is the active mode for InterviewLab outside of Phase 7-equivalent milestones.
- **Mode B — Self-run:** Claude reviews and guides only; owner writes the code. Switches only on explicit owner signal — never automatic.
- One step at a time. Never jump ahead to the next checkpoint without the current one being reviewed.
- Error handling, root cause first — never patch blindly (CLAUDE.md `MENTOR BEHAVIOUR`).
- Outdated patterns get flagged immediately with the current-LTS equivalent shown, not silently left in place.

## Token Discipline Rules

- Session notes capture command outputs and decisions as they happen (prepended, latest-first) so context isn't re-derived from scratch across sessions — see `session-notes/InterviewLab-session-notes-v*.txt`.
- Mistakes that cost significant token spend get logged with an approximate cost in `docs/mentorship/mistakes-and-fixes.md` — not to assign blame, but so the *pattern* that caused the waste (e.g. "verify the free tier before wiring in a provider") becomes a standing rule instead of a one-time apology.
- ForgeKit (`TheCodeForgeWorkspace/ForgeKit/FORGEKIT_PROMPT.txt`) is loaded only on-demand via the `forgekit` trigger keyword — zero token cost unless explicitly invoked.
- `CLAUDE.md` inheritance chain (workspace → TakshilaAI → InterviewLab) means only the project-level file needs to be read per session — parent rules apply automatically without re-reading parent files.

## Branching and Commit Format

See `docs/workflow.md` → also `CLAUDE.md` → `## BRANCHING STRATEGY` for the authoritative, up-to-date policy.

Internal (private repo / development) commit format:
```
[MODULE][TYPE] Description
```
Types: `FEAT` / `FIX` / `REFACTOR` / `TEST` / `DOCS` / `CHORE`
Example: `[JOB][FEAT] Add paginated job search`

Public repo commits (after Open Source Release, see CLAUDE.md `OPEN SOURCE RELEASE STANDARD`) use natural-English, action-verb-first messages instead — no bracketed module/type prefix, no internal checkpoint/phase numbering exposed:
```
Add Google OAuth2 login with JWT session management     ✓ good
[AUTH][FEAT] Google OAuth2 + JWT auth layer              ✗ bad — internal format leaking into public history
```

A commit message is required before every logical commit — CLAUDE.md `MENTOR BEHAVIOUR`: *"Commit message before every logical commit."* No exceptions, no "I'll write it after."

See also: [[decisions/ADR-001-ollama-over-gemini]] (example of a decision → ADR → code flow), [[mentorship/mistakes-and-fixes]]
