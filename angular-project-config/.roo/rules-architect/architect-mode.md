# Roo Code — Architect Mode Rules (Angular 21)

When planning in Architect mode:

## Frame every plan against the v21 defaults
- Standalone components everywhere
- Signals as primary reactivity primitive
- Zoneless change detection (`provideZonelessChangeDetection()`)
- Vitest as the test runner
- HttpClient auto-provided; interceptors via `provideHttpClient(withInterceptors([...]))`
- Functional guards/resolvers/interceptors

## Decision checklist for any plan
- Where does state live? component signal, service signal, or SignalStore?
- Is the data fetched reactively? Prefer `httpResource()` / `resource()` over manual subscribes
- Smart vs. presentational split — name the components on both sides
- Are routes lazy loaded? Identify the lazy boundary up front
- SSR / hydration implications? Mark `@defer (hydrate on viewport)` candidates
- Test boundaries: which units get specs, which get integration tests, which get E2E

## Migration plans (Angular 18 → 21)
- Follow `docs/MIGRATION_18_TO_21.md` step ordering — never skip a major
- Stage schematics after each major bump
- Land each major in its own PR with passing CI
- Don't bundle zoneless flip with the v21 bump unless the team has already audited `OnPush` coverage and Zone.js-dependent libs

## Output an Architect plan as
1. Goal & success criteria
2. Affected layers (`core`, `shared`, `features/...`, routes, providers)
3. New files (with v21 naming) and modified files
4. Data flow narrative (route → guard → component → service → API)
5. Test strategy (unit, integration, E2E)
6. Risks & rollback approach

## Don't write code in Architect mode
- Switch to Code mode for implementation
- Stay in Architect mode for diagrams, sequencing, and trade-off analysis
