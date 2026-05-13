# Roo Code — Debug Mode Rules (Angular 21)

When diagnosing in Debug mode:

## Common v21 footguns
- **Hydration mismatch**: browser-only API used during initial render → wrap with `isPlatformBrowser()` or `afterNextRender()`
- **"Expression has changed after it was checked"**: a non-OnPush component or untracked signal mutation; check `ChangeDetectionStrategy` and ensure mutations go through `signal.set/update`
- **NullInjectorError for `HttpClient`**: stale `HttpClientModule` import — replace with `provideHttpClient(withInterceptors([...]))`
- **`effect()` running unexpected times**: in v20+, effects run after CD. Use `computed()` for derived state; reserve `effect()` for true side effects
- **Test fails only in CI**: leftover Karma artifacts. Confirm `karma.conf.js` is gone and `ng test` is running Vitest
- **Zoneless app freezes after async**: a third-party library relies on Zone.js. Either upgrade the lib, wrap its callbacks with an explicit `signal.set()`, or stay zone-based for now
- **`@for` does not re-render after array mutation**: `track` expression collides with reused objects; use a stable id or `track $index` for primitives

## Investigation order
1. Reproduce — write a minimal failing test in Vitest first
2. Inspect signal graph — `console.log(signal())` at boundaries
3. Check provider configuration in `app.config.ts`
4. Check for legacy patterns flagged in `.roo/rules/10-banned-patterns.md`
5. Bisect with `git bisect` if regressions appeared after a v21 schematic

## Don't refactor in Debug mode
- File the fix, write the regression test, and stop. Larger cleanups go through Architect → Code mode.
