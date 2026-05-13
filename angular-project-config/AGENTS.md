# AGENTS.md — Angular Project (Angular 21, 2026)

> Canonical agent rules for this Angular workspace. All AI assistants
> (Claude Code, Roo Code, Cursor, Cline, Copilot, Gemini, Windsurf, etc.)
> must read and apply these rules. Editor-specific shims at the repo root
> simply re-point to this file.

## Project Overview
Angular 21 application using standalone components, signals-first reactivity,
zoneless change detection by default, lazy-loaded routes, Signal Forms (with
typed Reactive Forms as the stable fallback), Vitest as the test runner, and
TypeScript 5.9+ strict mode. Feature-based architecture with `core/shared/features` separation.

For full business context and domain rules, see:
- `docs/PROJECT.md` — business domain, entities, rules, integrations, scale
- `docs/ARCHITECTURE.md` — system design, component patterns, state management
- `docs/CONVENTIONS.md` — coding standards, naming, structure, testing
- `docs/TESTING.md` — testing strategy, test pyramid, coverage requirements
- `docs/MIGRATION_18_TO_21.md` — step-by-step Angular 18 → 21 upgrade path

## Build & Test
```bash
ng serve                                # Dev server
ng build                                # Production build (AOT)
ng test                                 # Run tests (Vitest by default in v21)
ng test --coverage                      # Tests with coverage (Vitest flag)
ng lint                                 # Lint check
ng generate component features/X/Y      # Standalone is the default in v21
ng update                               # Run scheduled migration schematics
```

Migration helpers shipped with Angular 21:
```bash
ng generate @angular/core:karma-to-vitest        # Migrate Karma → Vitest
ng generate @angular/core:control-flow           # *ngIf/*ngFor → @if/@for
ng generate @angular/core:standalone             # NgModule → standalone
ng generate @angular/core:signal-input-migration # @Input() → input()
ng generate @angular/core:output-migration       # @Output() → output()
ng generate @angular/core:signal-queries-migration # ViewChild → viewChild()
ng generate @angular/core:inject-migration       # constructor DI → inject()
ng generate @angular/core:cleanup-unused-imports # Remove dead imports
```

## Architecture

### Structure
- `src/app/core/` — singleton services, guards, interceptors, global models
- `src/app/shared/` — reusable stateless components, directives, pipes
- `src/app/features/` — feature areas (lazy loaded), each self-contained
- `src/app/layouts/` — layout components (header, sidebar, footer)

### Patterns
- Feature-based organization: each feature self-contained with components, services, models, routes
- Smart/presentational split: smart components orchestrate logic, presentational components are pure I/O
- Core services: `providedIn: 'root'` (singletons)
- Shared components: stateless, no service injections
- All feature routes lazy loaded from `app.routes.ts`

## Components — Angular 21 Best Practices

### Standalone & Signals-First
- Standalone is the **default** in v21 — never set `standalone: false`, never create new NgModules
- Signals-first for state: `signal()`, `computed()`, `effect()`, `linkedSignal()`
- Use `input()`, `input.required()`, `model()`, and `output()` signal APIs (not `@Input`/`@Output` decorators)
- Use `viewChild()`, `viewChildren()`, `contentChild()`, `contentChildren()` signal queries (not decorators)
- Use `inject()` for DI in functions, guards, resolvers, and prefer it in constructors too
- Use `@let` for template-local variables instead of intermediate pipes/methods
- `OnPush` change detection on all components (zoneless-ready)
- Separate `.html` and `.scss`/`.css` files for components > 10 lines

### Async Data & Side Effects
- Use `resource()` and `httpResource()` for reactive async data loading (stable in v21)
- Use `DestroyRef` or `takeUntilDestroyed()` for cleanup of observables
- Use `toSignal()` and `toObservable()` for interop between signals and observables
- Use `effect()` for side effects (logging, analytics, UI updates) — never for state derivation (use `computed()`)
- Use `afterRenderEffect()` for DOM-dependent side effects

### Templates
- Use `@let` for template-local variables and computed values
- Use `@for` with `track` expression (not legacy `*ngFor` / `trackBy` function)
- Use `@if/@else if/@else` for conditional rendering (not legacy `*ngIf`)
- Use `@switch/@case/@default` for branching (not legacy `*ngSwitch`)
- Use `@defer` (with `when`, `on idle`, `on viewport`, `on interaction`, `on hover`, `on timer`) for non-critical UI
- Use `[class.foo]` and `[style.bar]` bindings (avoid `ngClass`/`ngStyle` — soft-deprecated in v21)
- Selectorless components (experimental in v21): import a component class and use `<UserProfile />`-style tags in templates of components that opt-in via `selectorless: true`. Reserve for greenfield code while feature stabilizes.

### Smart vs. Presentational Split
- **Smart components**: inject services, manage state via signals, orchestrate data flow
- **Presentational components**: only `input()`/`output()`/`model()`, zero service injection, pure I/O

## Code Style

### TypeScript & File Structure
- TypeScript 5.9+ strict mode, never use `any` (use `unknown` + narrowing instead)
- **New Angular 21 naming style (preferred for new code)**: drop type suffixes — file name reflects class name
  - Component: `user-profile.ts` exporting `class UserProfile` with `user-profile.html`, `user-profile.scss`
  - Service: `order-api.ts` exporting `class OrderApi` (name reflects responsibility, not "service")
  - Guard: `auth-guard.ts` exporting `authGuard` function
  - Interceptor: `auth-interceptor.ts` exporting `authInterceptor` function
- **Legacy projects**: legacy `.component.ts` / `.service.ts` suffixes remain supported — keep them consistent within a project; migrate file-by-file only when convenient
- Exceptions to the suffix-less rule: **Pipes** (`title-case.pipe.ts`) and **NgModules** (`feature.module.ts`) still keep their suffixes
- Selectors stay kebab-cased with project prefix: `app-user-profile`
- Organize imports: builtins, third-party, local (separated by blank line)

### Forms & State
- **Signal Forms** (experimental in v21): use `form()` API for new forms when team is comfortable with experimental status; otherwise stick with typed Reactive Forms
- **Typed Reactive Forms** (stable): `FormGroup<T>`, `FormControl<T>` for complex forms
- Template-driven forms only for trivial single-input cases
- Use signals (`signal`, `computed`, `linkedSignal`) for synchronous state
- Use RxJS observables for async streams (not for state — convert with `toSignal()`)

### API Integration
- Centralized `ApiService` wrapping `HttpClient` (never call `HttpClient` directly from components)
- `HttpClient` is auto-provided in v21 — no need to call `provideHttpClient()` for default behavior, but still call it explicitly to register interceptors with `withInterceptors([...])`
- Use `httpResource()` for GET requests with reactive parameters (auto-tracks signal dependencies)
- Use `resource()` for custom async operations with signal-based parameters
- Type all HTTP request/response payloads with interfaces
- Use **functional** interceptors (not class-based) for auth, error handling, loading states

### Accessibility (new in v21)
- Use the new `@angular/aria` directives for accessible widgets (combobox, listbox, menu, tabs, dialog, etc.) instead of hand-rolling ARIA
- Always pair interactive elements with proper roles, labels, and keyboard handling

## HTTP & State Management

### ApiService Pattern
- Single `ApiService` that wraps `HttpClient`
- All feature/core services use `ApiService` for HTTP calls
- Services expose signal-based state and provide methods for mutations

### Reactive Data Loading
- Use `httpResource()` for GET endpoints that depend on signal parameters
- Use `resource()` for custom async operations with signal inputs
- Example pattern:
  ```typescript
  orders = httpResource(() => `/api/orders/${this.params.orderId()}`);
  // or with full config
  order = httpResource(() => ({
    url: `/api/orders/${this.params.orderId()}`,
    parse: (json) => json as Order,
  }));
  ```

### Functional Interceptors
- Auth: inject token and add to headers
- Error handling: log errors, transform error payloads
- Loading state: expose loading signal
- All as standalone functional interceptors registered via `withInterceptors([...])`

### State & Caching
- Simple state: service-level signals with `computed()` for derived state
- Complex state: NgRx SignalStore (preferred in v21) for large feature state
- Caching: signal-based patterns (cache in service, invalidate on mutation)

## Routing

### Lazy Loading
- All feature routes lazy loaded (never eager load features)
- Routes defined in `app.routes.ts` with `loadComponent` and `loadChildren`
- Each feature defines its own feature routes file

### Functional Guards & Resolvers
- Use functional guards (not class-based)
- Use functional resolvers (not class-based)
- Guards handle auth, permissions, data preload
- Resolvers fetch data before component renders

### SSR & Hybrid Rendering
- Configure render mode per route if using SSR/SSG via `provideServerRoutesConfig`
- Use `RenderMode.Server | RenderMode.Client | RenderMode.Prerender` per route
- **Incremental hydration** (stable in v21): wrap below-the-fold sections in `@defer (hydrate on viewport)`
- Built-in event replay during hydration — no manual setup needed in v21
- Preload routes with `withPreloading(PreloadAllModules)` or a custom strategy

## Testing

- **Default test runner: Vitest** (v21 default — Karma is removed for new projects)
- Migrate existing Karma config with `ng generate @angular/core:karma-to-vitest`
- Unit tests for services, pipes, presentational components
- Integration tests for smart components with templates and signals
- E2E with Cypress or Playwright
- Target 80% line coverage on services, 70% on components
- Use `TestBed`, `HttpTestingController`, and Angular Testing Library for clean tests
- Mock services and HTTP at the network boundary
- Run: `ng test` and `ng test --coverage`

## Performance

### Change Detection & Zoneless
- **Zoneless by default in v21**: new projects use `provideZonelessChangeDetection()` and omit Zone.js from `polyfills`
- Keep `OnPush` on every component — required for zoneless correctness
- Avoid `setTimeout`/`setInterval` for triggering CD; signals do this automatically

### Rendering & Lazy Loading
- Lazy load all feature routes
- Use `@defer` blocks for non-critical UI (below fold, modals, charts, etc.)
- Use `@for` with `track` expression for lists (prevents DOM reconstruction)
- Use `computed()` to derive reactive state efficiently
- Prefer `[class.xxx]` / `[style.xxx]` over `ngClass`/`ngStyle`

### Hydration & Serialization
- Hydration is the default — ensure components are SSR-safe
- Avoid browser-only APIs during initial render — gate them with `isPlatformBrowser()` or `afterNextRender()`
- Use `afterNextRender()` / `afterRender()` for browser-only setup

## Git Workflow

### Commits
- Format: `type(scope): description` (e.g., `feat(orders): add order detail view`)
- Types: feat, fix, refactor, test, docs, perf, ci, chore
- Scope: feature name or area
- Description: lowercase, imperative mood, no period

### Pull Requests
- Size: < 400 lines (excluding tests), must include tests
- Title: same format as commit
- Description: why the change, what was changed
- Link related issues

## Banned Patterns

### Architecture
- Do not create NgModules for new code (standalone is the default in v21)
- Do not set `standalone: false` on new components
- Do not put logic in presentational components
- Do not call other feature services directly (use core services)

### TypeScript & Templates
- Do not use `any` type — use `unknown` and narrow
- Do not use `::ng-deep` CSS selector
- Do not use `@Input()` / `@Output()` decorators (use `input()` / `output()` / `model()`)
- Do not use `@ViewChild` / `@ContentChild` decorators (use signal queries)
- Do not use `*ngIf` / `*ngFor` / `*ngSwitch` (use `@if` / `@for` / `@switch`) — deprecated in v21
- Do not use `ngClass` / `ngStyle` (use `[class.x]` / `[style.x]`) — soft-deprecated in v21
- Do not use `trackBy` function (use `track` expression in `@for`)
- Do not use `bypassSecurityTrustHtml` without security review

### Forms & State
- Do not use template-driven forms for complex forms
- Do not subscribe without cleanup (`takeUntilDestroyed()` or `DestroyRef`)
- Do not hardcode API URLs (use environment config)
- Do not expose mutable internals as public state
- Do not store derived state in `effect()` — use `computed()`

### HTTP & Async
- Do not call `HttpClient` directly from components
- Do not manually subscribe in components when `httpResource()` fits
- Do not use class-based interceptors (use functional)
- Do not use class-based guards/resolvers (use functional)
- Do not call `provideHttpClientModule()` (legacy) — use `provideHttpClient(withInterceptors([...]))`

### Testing
- Do not author new test specs against Karma — use Vitest
- Do not test framework internals
- Do not mock what you don't own without a thin wrapper

## Common Tasks

### New Feature
1. Create `src/app/features/{name}/` directory
2. Add components, services, models, routes subdirectories
3. Create smart container component and lazy routes
4. Define lazy route in `app.routes.ts` with `loadComponent` or `loadChildren`
5. Add tests alongside code

### New Shared Component
1. Create in `src/app/shared/components/{name}/`
2. Standalone (default), `OnPush`, inputs/outputs only
3. No service injection (pure I/O)
4. Add tests

### New Core Service
1. Create in `src/app/core/services/{name}.ts` (new convention) or `{name}.service.ts` (legacy)
2. Add `providedIn: 'root'` for singleton
3. Expose signal-based state and methods
4. Use `ApiService` for HTTP

### New Route Guard
1. Create functional guard in `src/app/core/guards/{name}-guard.ts`
2. Inject services with `inject()`
3. Return `boolean | UrlTree | Observable<boolean | UrlTree> | Promise<...>`
4. Add to route config via `canActivate: [authGuard]`

### Migrating an existing project from Angular 18
See `docs/MIGRATION_18_TO_21.md` for the full sequenced playbook. Short version:
1. `ng update @angular/core@19 @angular/cli@19` then `@20` then `@21` (one major at a time)
2. Run schematics after each major: control-flow, standalone, signal-input, output, signal-queries, inject, karma-to-vitest
3. Switch to zoneless via `provideZonelessChangeDetection()` once components are all `OnPush`
4. Adopt `httpResource()` / `resource()` for new data fetching
5. Consider Signal Forms only for greenfield forms while it's experimental
