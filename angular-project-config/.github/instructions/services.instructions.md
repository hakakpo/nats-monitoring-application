---
applyTo: "src/app/core/services/**/*.ts,src/app/features/**/services/**/*.ts"
---
# Angular Services — Angular 21

Follow all rules in `AGENTS.md`. Key points for services:

## Providers & State
- `providedIn: 'root'` for core singletons
- Business logic lives in services, not components
- Expose signal-based state; use `computed()` for derivations
- Inject dependencies with `inject()`

## Async Data & HTTP
- `httpResource()` for reactive GET requests with signal parameters
- `resource()` for custom async operations with signal inputs
- HttpClient (via centralized `ApiService`) for mutations (POST/PUT/DELETE)
- `HttpClient` is auto-provided in v21; call `provideHttpClient(withInterceptors([...]))` to register functional interceptors
- Functional interceptors only (no class-based)
- Type all request/response payloads with interfaces

## Caching & Interop
- Cache with signal-based patterns; invalidate on mutation
- Use `toSignal()` / `toObservable()` for signal/observable interop
- Keep service API clean and documented

## Naming (v21 style guide)
- Prefer responsibility-driven names: `OrderApi`, `AuthStore`, `CartTotals`
- File: `order-api.ts`, class `OrderApi`
- Legacy `.service.ts` suffix only when matching existing convention

## Anti-patterns
- No direct `HttpClient` usage in components (use `ApiService`)
- No class-based interceptors (use functional)
- No exposed mutable internals
- No duplicate error handling in services (interceptors own it)
- No cross-feature service calls (route through core services)
- No `HttpClientModule` imports (use `provideHttpClient(...)`)
