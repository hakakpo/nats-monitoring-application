# Roo Code — Code Mode Rules (Angular 21)

When implementing in Code mode:

## Before writing code
- Check `AGENTS.md` and `docs/CONVENTIONS.md` for naming rules
- Look for an existing `CLAUDE.md`/`AGENTS.md`/`README.md` in the directory you're editing — scoped rules override global
- Prefer `ng generate` over handwriting boilerplate (component, service, guard, interceptor, pipe, directive, resolver)

## Component scaffolding
- Standalone by default — never write `standalone: false`
- Always `OnPush` change detection
- Signal-based inputs/outputs: `input()`, `input.required()`, `output()`, `model()`
- Separate `.html` and `.scss`/`.css` for components > 10 lines
- File naming: prefer new v21 convention (`user-profile.ts`, class `UserProfile`) unless the project is mid-migration

## Service scaffolding
- `providedIn: 'root'` for core singletons
- Expose signal-based state; methods for mutations
- Inject dependencies with `inject()`
- Wrap HTTP via the centralized `ApiService`
- Use `httpResource()` for reactive GETs; `resource()` for custom async

## Templates
- `@if` / `@else if` / `@else` for conditionals
- `@for (item of items; track item.id) { ... }` for lists
- `@switch` / `@case` / `@default` for branching
- `@defer (on viewport) { ... }` for non-critical UI
- `@let derived = computeFrom(input());` for template-local values
- `[class.x]` / `[style.y]` over `ngClass`/`ngStyle`

## After writing code
- Run `ng test` (Vitest) to verify nothing is broken
- Run `ng lint`
- For production-bound changes: `ng build --configuration production` to catch TS strict-mode regressions
- Confirm no `any` types were introduced

## File creation rules
- Component: `ng generate component features/X/components/Y`
- Service: `ng generate service core/services/Z`
- Guard: `ng generate guard core/guards/auth --functional`
- Interceptor: `ng generate interceptor core/interceptors/auth --functional`
- Pipe: `ng generate pipe shared/pipes/title-case`
- Directive: `ng generate directive shared/directives/tooltip`
