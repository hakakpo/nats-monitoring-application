# CLAUDE.md — Angular 21 Project

Read and follow all project rules defined in `AGENTS.md` at the repository root.

For detailed reference, also consult the `docs/` directory:
- `docs/PROJECT.md` — business domain, entities, rules
- `docs/ARCHITECTURE.md` — system design, patterns
- `docs/CONVENTIONS.md` — coding standards, naming
- `docs/TESTING.md` — testing strategy, coverage
- `docs/MIGRATION_18_TO_21.md` — Angular 18 → 21 upgrade playbook

## Claude-Specific Instructions

- When working in a subdirectory, check for a local `CLAUDE.md` with scoped rules.
- Use the Angular CLI for scaffolding (`ng generate`) before writing components manually.
- When refactoring, verify no `any` types are introduced with `ng build --configuration production`.
- Run `ng test` (Vitest in v21) after every change to verify nothing is broken.
- When asked to upgrade an Angular 18 project, follow `docs/MIGRATION_18_TO_21.md` step by step — never skip a major version.
- Prefer the new v21 naming convention for new files (`user-profile.ts` exporting `UserProfile`); keep legacy `.component.ts` suffixes only when matching an existing convention in the same directory.
