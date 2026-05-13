# Migration Guide — Angular 18 → 21

This guide is for AI agents and humans driving the upgrade of an Angular 18
codebase to Angular 21. **Never jump majors**: upgrade one major at a time
(18 → 19 → 20 → 21), run schematics, fix breakages, commit, then move on.

> Always consult `https://angular.dev/update-guide` for the official, version-pinned
> commands — this document explains the **sequence and rationale**.

## Pre-flight Checklist

Before starting any upgrade:
- [ ] All tests pass on Angular 18
- [ ] CI is green on the current branch
- [ ] Branch is up to date with `main`
- [ ] Node.js ≥ 20.19 / ≥ 22.12 installed (v21 requires modern Node)
- [ ] TypeScript already on 5.5+ (v21 ships with 5.9)
- [ ] No `// @ts-ignore` you've been hiding — fix them now
- [ ] Bundle size baseline captured (`ng build --stats-json`)
- [ ] List of custom Karma plugins or Webpack hacks (you'll rewrite these for Vite)

## Step 1 — Angular 18 → 19

```bash
npx ng update @angular/core@19 @angular/cli@19
npx ng update @angular/material@19   # if you use Material
```

What landed in 19:
- Standalone is the default in `ng generate`
- `linkedSignal()` introduced
- `resource()` / `httpResource()` experimental
- Incremental hydration (`@defer (hydrate on ...)`) developer preview

Run these schematics:
```bash
npx ng generate @angular/core:standalone           # convert remaining NgModules
npx ng generate @angular/core:control-flow         # *ngIf/*ngFor → @if/@for
npx ng generate @angular/core:inject-migration     # constructor DI → inject()
```

Verify: `ng build && ng test && ng lint`. Commit.

## Step 2 — Angular 19 → 20

```bash
npx ng update @angular/core@20 @angular/cli@20
npx ng update @angular/material@20
```

What landed in 20:
- `resource()` / `httpResource()` **stable**
- New style guide: suffix-less file/class names (opt-in)
- `effect()` runs after change detection (semantic refinement — review your effects)
- Zoneless developer preview promoted closer to stable

Run these schematics:
```bash
npx ng generate @angular/core:signal-input-migration    # @Input() → input()
npx ng generate @angular/core:output-migration          # @Output() → output()
npx ng generate @angular/core:signal-queries-migration  # @ViewChild → viewChild()
npx ng generate @angular/core:cleanup-unused-imports
```

Replace data-fetching code:
- `HttpClient.get(...).subscribe(...)` in components → `httpResource(() => url)`
- Manual `loading`/`error` state in services → drop in favor of `resource()`'s built-in state

Adopt the new naming convention only for **new files** at this stage — wholesale rename is noisy. Run lint and tests, commit.

## Step 3 — Angular 20 → 21

```bash
npx ng update @angular/core@21 @angular/cli@21
npx ng update @angular/material@21
```

What landed in 21 (the big one):
- **Vitest is the default test runner** — Karma support is removed
- **Zoneless is the default** for new projects (and recommended for upgrades)
- **HttpClient auto-provided** in the root injector
- **Signal Forms** experimental (`form()` API)
- **Selectorless components** experimental (opt-in per component)
- **`@angular/aria`** directives for accessible widgets
- `*ngIf`/`*ngFor`/`*ngSwitch` officially **deprecated**
- `ngClass`/`ngStyle` **soft-deprecated**
- TypeScript 5.9 required

### 3a. Migrate the test runner
```bash
npx ng generate @angular/core:karma-to-vitest
```
Then:
- Delete `karma.conf.js`, `test.ts` (Karma entry), Karma reporter packages
- Replace `jasmine.createSpy()` with `vi.fn()` (codemod usually does this)
- If you have custom Karma webpack tweaks, port them to Vite plugins
- Run `ng test` — fix any failing specs

### 3b. Switch on zoneless
In `app.config.ts`:
```typescript
import { provideZonelessChangeDetection } from '@angular/core';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    // ...rest
  ],
};
```
Remove `zone.js` from the `polyfills` array in `angular.json`. **Required prerequisites:**
- All components are `OnPush` (or zoneless-safe)
- No code relies on `NgZone.run`/`runOutsideAngular` for CD triggering
- Third-party libs that rely on Zone.js (older RxJS-WebSocket wrappers, some chart libs) are upgraded or wrapped with explicit signal updates

If a third-party blocker exists, stay on zone-based CD for now — the schematics still work in zone mode.

### 3c. Remove deprecated structural directives (optional but recommended)
Run again to catch anything new the team wrote during the v20 phase:
```bash
npx ng generate @angular/core:control-flow
```
Replace `ngClass`/`ngStyle` manually — there is no automated codemod yet:
- `[ngClass]="{ active: isActive }"` → `[class.active]="isActive"`
- `[ngStyle]="{ color: textColor }"` → `[style.color]="textColor"`

### 3d. Update HTTP bootstrap
If you still call legacy `HttpClientModule` imports, remove them. In `app.config.ts`:
```typescript
import { provideHttpClient, withInterceptors } from '@angular/common/http';

providers: [
  provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
]
```
You can omit `provideHttpClient()` for default behavior, but include it explicitly to register interceptors.

### 3e. Adopt new naming convention (optional, gradual)
For each touched file, rename:
- `order-list.component.ts` → `order-list.ts`, class `OrderListComponent` → `OrderList`
- `order.service.ts` → `order-api.ts`, class `OrderService` → `OrderApi`
Update imports and selectors do **not** change (`app-order-list` stays).

Keep pipes (`.pipe.ts`) and any remaining modules (`.module.ts`) suffixed.

### 3f. Try Signal Forms (greenfield only)
Don't migrate existing reactive forms — Signal Forms are still experimental in v21.
Use them for new forms and keep `FormGroup<T>` for stable critical paths.

### 3g. Try selectorless components (experimental)
Opt-in per component with `selectorless: true`. Use the class name directly as a tag in templates of components that also opt-in. Confine the experiment to a single greenfield feature first.

## Step 4 — Post-Upgrade Hygiene

- [ ] Run `ng lint` — fix any new warnings
- [ ] Run `ng build --configuration production` — check bundle size deltas
- [ ] Run full test suite under Vitest
- [ ] Update the project's `AGENTS.md` to reflect any local divergences
- [ ] Update CI: bump Node version, swap Karma cache for Vitest cache
- [ ] Capture screenshots / smoke tests against staging before merging

## Common Pitfalls

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `effect()` runs unexpected times | v20+ effects run after CD | Review timing; use `computed()` for derived state |
| Tests pass locally, fail in CI | Karma artifacts left behind | Delete `karma.conf.js` and Karma deps |
| Hydration mismatch errors | Browser-only code in templates | Gate with `isPlatformBrowser()` or `afterNextRender()` |
| Random "Expression has changed" errors | A non-OnPush component during zoneless attempt | Ensure all components are OnPush before flipping to zoneless |
| HttpClient `NullInjectorError` | Stale `HttpClientModule` import | Replace with `provideHttpClient()` |
| Linter flags `*ngIf` | v21 deprecation warning | Run `control-flow` schematic |
| `trackBy: trackByFn` warning | Should use `track` expression | Update `@for` to use `track item.id` |

## Rollback Plan

Each major upgrade should land in its own PR with passing CI. Keep the previous
`package-lock.json` around — if a downstream surprise appears (e.g. a chart
library breaks under zoneless), you can revert the single PR rather than the
whole upgrade train.
