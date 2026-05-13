---
applyTo: "src/app/features/**/*.ts,src/app/features/**/*.component.ts"
---
# Feature Components — Angular 21

Follow all rules in `AGENTS.md`. Key points for feature components:

## Standalone & Signals-First (v21 defaults)
- Standalone is the default — never set `standalone: false`
- `OnPush` change detection on every component (required for zoneless)
- Signals-first: `signal()`, `computed()`, `effect()`, `linkedSignal()`
- `input()`, `input.required()`, `output()`, `model()` signal APIs (not decorators)
- Signal queries: `viewChild()`, `viewChildren()`, `contentChild()`, `contentChildren()`
- `resource()` / `httpResource()` for async data (stable in v21)
- Inject dependencies with `inject()`

## Templates
- `@let` for template-local variables
- `@for (x of xs; track x.id)` for lists (not `*ngFor` / `trackBy`)
- `@if` / `@else if` / `@else` for conditionals (not `*ngIf`)
- `@switch` / `@case` / `@default` for branching (not `*ngSwitch`)
- `@defer (on viewport|idle|interaction|...)` for non-critical sections
- `[class.x]` / `[style.y]` over `ngClass` / `ngStyle`
- Selectorless components (experimental): opt in per component, reserve for greenfield

## Smart vs Presentational
- Smart components: inject services, manage state, fetch data via `resource()` / `httpResource()`
- Presentational components: pure I/O, no services, signal inputs/outputs only
- Cleanup with `takeUntilDestroyed()` or `DestroyRef`

## File naming (v21 style guide)
- Prefer suffix-less names for new files: `order-list.ts` exporting `class OrderList`
- Legacy `.component.ts` only when matching the existing convention in the same directory

## Anti-patterns
- No business logic in presentational components
- No manual `HttpClient.subscribe` when `httpResource()` fits
- No `@Input()` / `@Output()` decorators
- No `@ViewChild` / `@ContentChild` decorators
- No legacy structural directives (`*ngIf`, `*ngFor`, `*ngSwitch`)
- No subscribing without cleanup
