# CODEX.md — Java Layered Architecture Project

> **Canonical rules live in `AGENTS.md`.** This file adds OpenAI Codex CLI-specific guidance.
> Read `AGENTS.md` first, then this file, then the `docs/` folder.

## IMPORTANT: Read These Files First

1. `AGENTS.md` — shared rules (single source of truth)
3. `docs/ARCHITECTURE.md` — layered design, dependency rules, package structure
4. `docs/CONVENTIONS.md` — coding standards, Java 21/25, error handling, logging
5. `docs/TESTING.md` — testing strategy per layer


## The #1 Rule

**Business logic belongs in the service layer, NOT in controllers or repositories.**

## Package Map
```
controller/   REST controllers (@RestController) — implement generated API interfaces
service/      Business logic (@Service, @Transactional)
repository/   Spring Data JPA repositories
model/        JPA entities (@Entity) and enums
mapper/       MapStruct interfaces (generated DTO ↔ entity)
exception/    Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
config/       Spring @Configuration classes
```

## What NOT To Do (top 10)
1. Do NOT put business logic in controllers — delegate to services
2. Do NOT inject repositories into controllers — always go through services
3. Do NOT return JPA entities from controllers — always use generated model DTOs
4. Do NOT use `@Autowired` on fields — use constructor injection
5. Do NOT skip `@Transactional` on service methods that write data
6. Do NOT catch generic `Exception` — catch specific types
7. Do NOT use `and()` in Spring Security (removed in SB4 — use Lambda DSL)
8. Do NOT use `System.out.println` — use SLF4J logger
9. Do NOT use `spring.jpa.hibernate.ddl-auto=update` in production
10. Do NOT return `null` — use `Optional<T>`

## Error Handling
RFC 9457 `ProblemDetail`. `GlobalExceptionHandler extends ResponseEntityExceptionHandler`.
Enable: `spring.mvc.problemdetails.enabled=true`.

## Testing
- `@Tag("unit")` on unit tests, `@Tag("integration")` on integration tests
- Services: JUnit 5 + Mockito
- Controllers: `@WebMvcTest` + `@MockBean OrderService`
- Repositories: `@DataJpaTest` + Testcontainers

## Build Commands
```bash
./mvnw test                   # All tests
./mvnw test -Dgroups=unit     # Unit only
./mvnw generate-sources       # Regenerate from openapi.yaml
./gradlew test
./gradlew openApiGenerate
```
