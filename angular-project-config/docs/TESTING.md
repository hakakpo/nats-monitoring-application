# Testing Guide — Angular 21

## Frameworks
- **Vitest + Angular Testing Library** is the default in Angular 21 (`ng test`)
- Karma + Jasmine is removed in v21 — migrate with `ng generate @angular/core:karma-to-vitest`
- **Cypress** or **Playwright** for end-to-end tests
- Use Angular testing utilities (`TestBed`, `HttpTestingController`, `fakeAsync`) when needed

## Testing Philosophy
- Test behavior, not implementation details
- Keep tests deterministic and independent
- Prefer user-observable outcomes over internal state assertions
- Mock network boundaries and external services

## Test Classification

### Unit Tests (co-located `*.spec.ts`)
- Services: test business logic and API wrappers
- Components: test rendering, inputs/outputs, UI interactions
- Pipes/directives: transformations and DOM behavior in isolation

```typescript
import { TestBed } from '@angular/core/testing';
import { describe, it, expect, beforeEach } from 'vitest';
import { CounterService } from './counter';

describe('CounterService', () => {
  let service: CounterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CounterService);
  });

  it('increments count', () => {
    service.increment();
    expect(service.count()).toBe(1);
  });
});
```

### Component Tests with signals

```typescript
import { render, screen } from '@testing-library/angular';
import { describe, it, expect } from 'vitest';
import { OrderList } from './order-list';

describe('OrderList', () => {
  it('renders the list of orders', async () => {
    await render(OrderList, {
      inputs: { orders: [{ id: 1, total: 100 }] },
    });
    expect(screen.getByText('100')).toBeInTheDocument();
  });
});
```

### Integration Tests (feature-level)
- Smart components with templates and real Angular bindings
- Verify collaboration between component, service, and routing state
- Include loading, empty, success, and error states
- Test signals and `resource()` data-loading lifecycle
- Verify signal effects and computed signal updates

### E2E Tests
- Validate critical user flows (auth, checkout, CRUD workflows)
- Run against production-like configuration
- Keep scenarios small and business-oriented

## Coverage Requirements
- Services: 80%+ line coverage
- Components: 70%+ line coverage
- Critical features: at least one integration or E2E scenario per flow

## What Not to Test
- Angular framework internals
- CSS implementation details unless behavior depends on them
- Trivial getters/setters without business value
- Signals' internal scheduling — test outputs, not effects' timing

## Running Tests
```bash
ng test                       # Vitest watch mode
ng test --watch=false         # Single run
ng test --coverage            # With coverage report
ng e2e                        # Cypress or Playwright (depending on setup)
```

## Migrating Karma → Vitest
```bash
ng generate @angular/core:karma-to-vitest
```
After running the schematic:
- Remove `karma.conf.js`, Karma-specific reporters, and Jasmine globals from `tsconfig.spec.json`
- Replace `jasmine.createSpy` with `vi.fn()` (the codemod handles most cases)
- Switch from `done()` callbacks to `async/await` or Vitest's `fakeTimers`
- Custom Karma plugins/webpack tweaks must be rewritten for Vite
