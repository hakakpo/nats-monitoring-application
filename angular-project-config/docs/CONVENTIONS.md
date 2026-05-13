# Angular 21 Coding Conventions

## Editor & Formatting
- `.editorconfig` at the repo root is the source of truth for editor formatting
- TypeScript/JavaScript/HTML/SCSS/CSS use 2-space indentation
- JSON/YAML/XML use 2-space indentation
- Line endings are `lf` and files end with a final newline
- Do not include broad formatting-only changes in mixed commits

## Framework & Language
- Angular 21 with standalone components (default — no NgModules for new code)
- TypeScript 5.9+ strict mode — never use `any` (use `unknown` + narrowing)
- Signals for reactive state (preferred over RxJS for component state)
- RxJS for async streams, complex event handling, and HTTP mutation pipelines
- Zoneless change detection by default (`provideZonelessChangeDetection()`)
- Vitest for unit tests (default in v21); Karma is removed

## Naming (Angular 21 style guide)

The v20/v21 style guide drops type suffixes from filenames and class names.
Adopt this for **new code**; legacy files can keep their suffixes until migrated.

### Files
| Kind          | New convention (v21)       | Legacy convention         |
|---------------|----------------------------|---------------------------|
| Component     | `user-profile.ts`          | `user-profile.component.ts` |
| Template      | `user-profile.html`        | `user-profile.component.html` |
| Styles        | `user-profile.scss`        | `user-profile.component.scss` |
| Service / API | `order-api.ts`             | `order.service.ts`        |
| Directive     | `tooltip.ts`               | `tooltip.directive.ts`    |
| Guard         | `auth-guard.ts`            | `auth.guard.ts`           |
| Interceptor   | `auth-interceptor.ts`      | `auth.interceptor.ts`     |
| Resolver      | `user-resolver.ts`         | `user.resolver.ts`        |
| Model         | `order.ts`                 | `order.model.ts`          |
| Pipe          | `title-case.pipe.ts`       | (same — pipes keep suffix)|
| NgModule      | `feature.module.ts`        | (same — modules keep suffix; new code shouldn't have them) |

### Classes
- Component class: `UserProfile` (not `UserProfileComponent`) — class name reflects responsibility
- Service class: name it for what it does — `OrderApi`, `AuthStore`, `CartTotals` — not `OrderService`
- Pipe class: keep `Pipe` suffix (`TitleCasePipe`) — exception
- Interfaces/Types: PascalCase, no `I` prefix (`OrderResponse`, not `IOrderResponse`)
- Constants: UPPER_SNAKE_CASE (`MAX_PAGE_SIZE`)
- Enums: PascalCase name, PascalCase members

### Selectors
- Components: kebab-case with project prefix (`app-user-profile`)
- Directives: camelCase with project prefix (`appTooltip`)

## Project Structure
```
src/app/
├── core/                    # Singleton services, guards, interceptors
│   ├── services/            # Application-wide services
│   │   ├── auth.ts          # AuthStore class
│   │   └── api.ts           # ApiService class
│   ├── guards/              # Functional route guards
│   ├── interceptors/        # Functional HTTP interceptors
│   └── models/              # Shared interfaces, types, enums
├── shared/                  # Reusable components, directives, pipes
│   ├── components/          # Presentational components
│   ├── directives/
│   └── pipes/
├── features/                # Feature-based areas (lazy loaded)
│   ├── orders/
│   │   ├── components/
│   │   │   ├── order-list/
│   │   │   │   ├── order-list.ts        # exports class OrderList
│   │   │   │   ├── order-list.html
│   │   │   │   ├── order-list.scss
│   │   │   │   └── order-list.spec.ts
│   │   │   └── order-detail/
│   │   ├── services/        # Feature-specific services
│   │   ├── models/          # Feature-specific types
│   │   └── orders.routes.ts # Feature routes
│   └── dashboard/
├── layouts/                 # Layout components (header, sidebar, footer)
├── app.routes.ts            # Root route configuration
├── app.config.ts            # App-level providers
└── app.ts                   # Root component
```

## Architecture Rules
1. **Smart vs presentational**: smart components handle logic and data; presentational components are pure input/output
2. **Feature-based organization**: each feature is self-contained with routes, components, services, models
3. **Core services are singletons**: `providedIn: 'root'`, never in component providers
4. **Shared components are stateless**: no service injections — only signal `input()`/`output()`/`model()`
5. **Lazy loading**: every feature route is lazy loaded

## Component Rules
- Standalone is the default — never set `standalone: false`
- Prefer signals (`signal`, `computed`, `effect`, `linkedSignal`) for component state
- Use `input()`, `input.required()`, `output()`, `model()` signal-based APIs
- Use signal queries: `viewChild()`, `viewChildren()`, `contentChild()`, `contentChildren()`
- Use `inject()` for DI inside constructors and factory functions
- Template: separate `.html` file for components > 10 lines
- Styles: component-scoped SCSS/CSS, separate file
- `OnPush` change detection on all components (required for zoneless)
- Cleanup observables with `takeUntilDestroyed()` or `DestroyRef`

## State Management
- **Simple state**: signals in services
- **Complex state**: NgRx SignalStore (preferred) or NgRx Store
- **Server state**: keep in services, cache with signals, invalidate on mutation
- Never mutate state directly — always create new references with `update()` / `set()`

## HTTP & API
- Centralized `ApiService` wrapping `HttpClient`
- `HttpClient` is auto-provided in v21; call `provideHttpClient(withInterceptors([...]))` to register interceptors
- Functional interceptors only: auth tokens, error handling, loading state
- Use `resource()` / `httpResource()` for reactive data fetching
- Type all HTTP responses with interfaces
- Use `src/environments/` files for API base URLs
- Handle errors in interceptor, not in every service method
- Use `toSignal()` / `toObservable()` for observable ↔ signal interop

## Forms
- **Signal Forms** (experimental in v21): adopt for new forms when comfortable with the experimental flag
- **Typed Reactive Forms** (stable): `FormGroup<OrderForm>` for complex stable-form work
- Template-driven forms only for trivial single-input cases
- Validation: built-in + custom validators in shared
- Error messages via a reusable error component

## Routing
- Lazy load all feature routes (`loadComponent` / `loadChildren`)
- Use functional resolvers (not class-based) only when data must exist before render
- Guard routes with functional guards (not class-based)
- Title strategy for page titles
- Use `withPreloading()` strategy for route preloading
- Use `withViewTransitions()` for animated route transitions when desired

## Testing
- **Framework**: Vitest + Angular Testing Library (default in v21)
- **Legacy**: Karma + Jasmine is removed in v21; migrate via `ng generate @angular/core:karma-to-vitest`
- **Unit tests**: components, services, pipes in isolation
- **Integration**: smart components with templates and signals/resource()
- **E2E**: Cypress or Playwright
- **Coverage**: 80% on services, 70% on components

## Styling
- SCSS or CSS with component-scoped styles (Angular emulates view encapsulation by default)
- Use CSS custom properties for theming
- **Never** use `::ng-deep` (deprecated)
- Follow BEM if not using utility-first CSS
- Responsive: mobile-first with breakpoint mixins
- Prefer `[class.foo]` / `[style.bar]` over `ngClass` / `ngStyle`

## Accessibility
- Use `@angular/aria` directives for accessible widgets (new in v21)
- Always provide accessible names for interactive elements
- Test with keyboard navigation and screen readers

## Performance
- `OnPush` on all components (required for zoneless)
- Lazy loading for features and heavy components
- `@defer` blocks for non-critical UI sections
- `track` expression in `@for` loops
- Preload strategy for likely-next routes
- `@let` template syntax for template variable caching

## Security
- Sanitize user input (Angular does this by default for templates)
- Never use `bypassSecurityTrustHtml` without review
- Use HttpOnly cookies for auth tokens when possible
- CSP headers configured at server level
- Environment-specific CORS configuration

## Git & Code Review
- Commit messages: `type(scope): description` (e.g., `feat(orders): add filter component`)
- PR size: < 400 lines of production code
- Every PR must include tests
- No `// @ts-ignore` without justification
