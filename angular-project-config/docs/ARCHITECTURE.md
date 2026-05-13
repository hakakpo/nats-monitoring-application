# Architecture Overview — Angular 21 Application

## System Design

This is a feature-based Angular 21 application using standalone components,
signals for reactivity, zoneless change detection by default, and lazy-loaded
routes. It follows smart/presentational component patterns with a clear
`core/shared/features` structure.

## Layer Diagram

```
┌─────────────────────────────────────────┐
│            Feature Areas                 │
│  Smart Components, Feature Services,    │
│  Feature Routes (lazy loaded)           │
├─────────────────────────────────────────┤
│            Shared Layer                  │
│  Presentational Components, Pipes,      │
│  Directives (stateless, reusable)       │
├─────────────────────────────────────────┤
│             Core Layer                   │
│  Singleton Services, Functional Guards, │
│  Functional Interceptors, Global Models │
├─────────────────────────────────────────┤
│          Infrastructure                  │
│  HttpClient, Router, External APIs      │
└─────────────────────────────────────────┘
```

## Key Patterns

### Data Flow
1. Route activates → Functional guard checks access → Functional resolver pre-fetches (rare; prefer `resource()` in component)
2. Smart component receives data via injected services (`inject()`)
3. Service uses `resource()` or `httpResource()` for reactive data fetching
4. Smart component passes data to presentational components via `input()` signals
5. User actions emit via `output()` signals → smart component handles logic
6. API calls update service signals → components react via signals and `computed()`

### Component Communication
- Parent → Child: `input()` signal inputs, `input.required()` for mandatory props
- Child → Parent: `output()` signal outputs
- Two-way: `model()` (signal-based equivalent of banana-in-a-box)
- Siblings/unrelated: shared service with signals
- Linked state: `linkedSignal()` to derive an editable signal from another signal
- Complex state: NgRx SignalStore

### Error Flow
1. HTTP error occurs
2. Error interceptor catches it
3. For auth errors (401/403): redirect to login
4. For server errors (5xx): show toast notification
5. For validation errors (400): surface to component via `httpResource().error()` or service signal

## Routing Strategy
- Root `app.routes.ts` defines top-level lazy routes
- Each feature has its own `feature.routes.ts`
- Guards: functional guards (not class-based) for auth and role checks
- Resolvers: functional resolvers when data must exist before render; otherwise let the component own loading via `httpResource()`
- SSR: render mode configured in route definitions with `RenderMode`
- Incremental hydration via `@defer (hydrate on ...)`

## State Management
- Component state: signals (`signal`, `computed`, `linkedSignal`)
- Feature state: service with signals and `resource()` for most features
- Complex/shared state: NgRx SignalStore
- Server cache: service-level caching with signals, invalidate on mutation
- Zoneless by default: components react to signal changes without `NgZone`

## API Integration
- Centralized `ApiService` wraps `HttpClient`
- Base URL from environment configuration
- Auth token injected via functional interceptor
- Retry logic for transient failures (RxJS `retry` operator inside `ApiService`)
- Response typing with interfaces
- `resource()` / `httpResource()` for automatic request/loading/error state

## Bootstrap & Providers (`app.config.ts`)

Typical v21 root config:

```typescript
export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withPreloading(PreloadAllModules), withViewTransitions()),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideClientHydration(withIncrementalHydration(), withEventReplay()),
  ],
};
```

## Build & Deploy
- Angular CLI builds with esbuild + Vite (default in v21)
- AOT compilation enabled by default
- Bundle-size budgets configured in `angular.json`
- Source maps disabled in production
- Environment-specific configuration in `src/environments/`
- SSR builds with `@angular/ssr` (set up via `ng add @angular/ssr` if needed)
