# Roo Code Project Rules — Angular 21

Roo Code automatically loads `AGENTS.md` at the workspace root and any
Markdown files under `.roo/rules/`. The canonical, exhaustive ruleset lives
in `AGENTS.md`; everything in this directory is either a Roo-specific
nuance or a topical breakout.

**Order of authority:**
1. Mode-specific rules in `.roo/rules-{mode}/` (e.g. `rules-code`, `rules-architect`)
2. `.rooignore`
3. `AGENTS.md` at repo root
4. Generic rules in `.roo/rules/` (this directory)

## Roo-specific behaviors

- Prefer Roo's **Code mode** for implementation; **Architect mode** for planning;
  **Ask mode** for explanation; **Debug mode** for diagnosis. Per-mode rules
  in `.roo/rules-{mode}/` will scope behavior automatically.
- When asked to scaffold, run Angular CLI commands (`ng generate ...`) rather
  than handwriting boilerplate.
- Always read `docs/CONVENTIONS.md` for naming before creating files.
- Always read `docs/MIGRATION_18_TO_21.md` before suggesting upgrade steps.

## Quick reference

- Standalone is default. No new NgModules.
- Signal-based APIs only: `input()`, `output()`, `model()`, `viewChild()`, signals.
- Built-in control flow: `@if`, `@for` (with `track`), `@switch`, `@defer`, `@let`.
- Vitest is the test runner. Karma is gone.
- Zoneless by default. `OnPush` everywhere.
- HttpClient is auto-provided; use `provideHttpClient(withInterceptors([...]))` for interceptors.
- Functional guards, resolvers, interceptors only.
- Centralized `ApiService` wraps `HttpClient`. Components never use `HttpClient` directly.

For everything else, follow `AGENTS.md`.
