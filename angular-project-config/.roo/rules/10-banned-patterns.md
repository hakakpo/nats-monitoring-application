# Banned Patterns — Roo Code Quick Card

Refuse or rewrite if a request would introduce any of the following:

## Architecture
- New `NgModule` (use standalone)
- `standalone: false` on new components
- Business logic inside presentational components
- One feature service calling another feature service directly

## TypeScript & Templates
- `any` type — use `unknown` and narrow
- `::ng-deep` selector
- `@Input()` / `@Output()` decorators — use `input()` / `output()` / `model()`
- `@ViewChild` / `@ContentChild` decorators — use signal queries
- `*ngIf` / `*ngFor` / `*ngSwitch` — use `@if` / `@for` / `@switch`
- `ngClass` / `ngStyle` — use `[class.x]` / `[style.x]`
- `trackBy` function — use `track` expression in `@for`
- `bypassSecurityTrustHtml` without explicit security review

## Forms & State
- Template-driven forms for complex forms
- Observables subscribed without `takeUntilDestroyed()` / `DestroyRef`
- Hardcoded API URLs (use environment config)
- Mutable internals exposed as public state
- `effect()` used to derive state (use `computed()`)

## HTTP & Async
- `HttpClient` called directly from a component
- Manual subscribe when `httpResource()` fits
- Class-based interceptors / guards / resolvers (use functional)
- `HttpClientModule` import (use `provideHttpClient(withInterceptors([...]))`)

## Testing
- New specs written against Karma (Vitest is the v21 default)
- Mocking framework internals
