# Roo Code — Ask Mode Rules (Angular 21)

When answering questions in Ask mode:

- Anchor explanations in **Angular 21 defaults**: standalone, signals, zoneless, Vitest, built-in control flow
- Distinguish stable APIs (`signal`, `computed`, `input`, `output`, `resource`, `httpResource`) from experimental ones (Signal Forms, selectorless components) when relevant
- When the user is on an older Angular version, mention the version gap and point them to `docs/MIGRATION_18_TO_21.md`
- Cite which doc the rule comes from when answering "why" questions (`AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/TESTING.md`)
- Don't write new code — link to existing files or sketch minimal examples in chat
- Prefer comparison tables for "X vs Y" questions (e.g. `httpResource()` vs `HttpClient.get()`, Signal Forms vs Reactive Forms)
