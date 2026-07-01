# ADR-003: CDN React over a bundled build

## Status
Accepted

## Context
InterviewLab's product scope explicitly includes third-party embeddability (see CLAUDE.md `PRODUCT SCOPE`). An integrator embedding InterviewLab should not need a Node toolchain, a build step, or a bundler config just to drop the widget into their page. A conventional Vite/webpack-bundled SPA requires exactly that: `npm install`, a build pipeline, and a hosting story for the compiled output.

## Decision
Serve React directly from CDN script tags (`react`, `react-dom` via CDN) with Babel's in-browser JSX transformer, no build step. The compiled frontend is static files under `src/main/resources/static/`, served by the same Spring Boot process that serves the API — one deployable, no separate frontend build pipeline.

## Options Considered
| Option | Verdict | Reason |
|---|---|---|
| Vite / webpack bundled SPA | Rejected | Requires a build step — breaks the "drop it in, no toolchain" embeddability requirement |
| **CDN React + in-browser Babel** | **Chosen** | Zero build step, works by pasting script tags, matches embeddability requirement directly |

## Consequences
- No ES module `import`/`export` — components must attach to `window.*` globals (`window.VoiceRecorder`, etc.) and be loaded in dependency order via `<script>` tags. This is a real footgun: it is easy to write `export default` out of habit and get a silent `ReferenceError` at runtime instead of a build-time error.
- No tree-shaking, no minification pipeline by default, no TypeScript type-checking at build time — correctness has to be verified by loading the page, not by a compiler
- In-browser Babel JSX transform costs parse time on every page load (only pays off because the app is small; would not scale to a large SPA)
- Simplicity win: any integrator can embed the widget with a handful of `<script>` tags and zero npm/build dependency

## Lesson
CDN setup requires explicit global-scope wiring — ES modules don't work without a bundler. Every component file needs to end with an explicit `window.ComponentName = ComponentName;` assignment, and script tag order in the host HTML must match the dependency graph by hand, since there's no bundler to resolve it.

## Interview Talking Point
"The embeddability requirement — no build step for third-party integrators — directly drove the frontend architecture decision. I chose CDN React with in-browser Babel over a bundled SPA specifically so the widget can be embedded with script tags, no npm toolchain required. The trade-off is manual dependency ordering and no tree-shaking, which is fine at this scale but wouldn't be the right call for a large standalone SPA."

See also: [[mistakes-and-fixes]]
