# AGENTS.md — Java Layered Architecture (Single Source of Truth)

> **This file is the canonical agent instruction set.** All tool-specific config files
> (`.cursorrules`, `.windsurfrules`, `.github/copilot-instructions.md`, `CLAUDE.md`)
> reference this file. Edit rules HERE, not in those files.

## Project Overview

Spring Boot 3.5.14 / Java 21 application using a **Classic Layered Architecture**
(Controller → Service → Repository). Spring framework conventions are embraced
throughout the codebase. Business logic lives in the service layer.

For full business context, entities, and rules, see `docs/PROJECT.md`.

## Required Reading (Before Any Change)

| File | What It Contains |
|------|-----------------|
| `docs/PROJECT.md` | Business domain, entities, rules, integrations, scale |
| `docs/ARCHITECTURE.md` | Layered design, dependency rules, package structure, code examples |
| `docs/CONVENTIONS.md` | Coding standards, naming, error handling, logging |
| `docs/TESTING.md` | Testing strategy per layer, coverage targets |
| `docs/adr/` | Architecture Decision Records — past decisions and rationale |

## Reference Implementation

The `src/` directory contains a **working vertical slice** (CreateOrder feature) that
demonstrates the correct layered pattern end-to-end. When implementing new features,
**follow the existing code as a template**:

```
src/main/resources/openapi/openapi.yaml  → API spec (edit this first for any new endpoint)
model/Order.java                         → JPA entity pattern
repository/OrderRepository.java          → Spring Data repository pattern
service/OrderService.java                → Service layer pattern
controller/OrderController.java          → REST controller (implements generated OrdersApi)
mapper/OrderMapper.java                  → MapStruct interface (DTO ↔ entity)
exception/OrderNotFoundException.java   → Domain exception pattern

Generated (do not edit):
  com.company.app.api.OrdersApi          → Generated controller interface
  com.company.app.api.model.*            → Generated request/response models
```

## The #1 Rule

**Business logic belongs in the service layer, NOT in controllers or repositories.**

Controllers handle HTTP concerns only (request validation, response mapping, status codes).
Repositories handle persistence only (queries, CRUD). Services contain all business rules,
orchestration, and transaction management.

## Package Map

```
controller/              → REST controllers (@RestController) — implement generated API interfaces
service/                 → Business logic (@Service, @Transactional)
repository/              → Data access (@Repository, Spring Data interfaces)
model/                   → JPA entities (@Entity) and enums
dto/                     → Hand-written DTOs if needed (prefer generated models in api/model/)
mapper/                  → MapStruct interfaces for DTO ↔ entity conversion (generated at compile time)
exception/               → Custom exceptions + global error handler
config/                  → Spring @Configuration classes
event/                   → Application events (optional)

Generated (never edit manually):
  (build)/generated-sources/openapi/
    com.company.app.api/            → Generated controller interfaces (one per OpenAPI tag)
    com.company.app.api.model/      → Generated request/response model classes
```

## OpenAPI Contract-First Workflow

This project follows a **contract-first** approach: the API spec drives code generation.

### Spec location

```
src/main/resources/openapi/openapi.yaml   ← single source of truth for the API contract
```

### What gets generated

Running `./mvnw generate-sources` (or `./gradlew openApiGenerate`) produces:

| Generated artifact | Package | How it is used |
|---|---|---|
| `OrdersApi` interface | `com.company.app.api` | `OrderController implements OrdersApi` |
| `CreateOrderRequest` model | `com.company.app.api.model` | Received by controller method parameter |
| `OrderResponse` model | `com.company.app.api.model` | Returned by controller method |

One interface is generated **per OpenAPI tag**. Use one tag per resource.

### Controller pattern — implement the generated interface

```java
// controller/OrderController.java
@RestController
@RequiredArgsConstructor
public class OrderController implements OrdersApi {   // ← implements generated interface

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @Override
    public ResponseEntity<OrderResponse> createOrder(
            @Valid CreateOrderRequest request) {         // ← generated model class
        var serviceResult = orderService.createOrder(
                orderMapper.toServiceInput(request));    // ← MapStruct converts it
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderMapper.toApiResponse(serviceResult));
    }
}
```

### Rules for generated code

- **Never edit files under `target/generated-sources/` or `build/generated/`** — they are
  overwritten on every build.
- The `openapi.yaml` spec is the only file you edit to change the contract.
- After editing the spec, regenerate and recompile: `./mvnw generate-sources compile`.
- If the generated interface changes (e.g., new method added), update the implementing
  controller to satisfy the interface.
- Do NOT commit generated sources to git — add the output directories to `.gitignore`.

## MapStruct Object Mapping

MapStruct generates **type-safe mapper implementations** at compile time from annotated
interfaces. It replaces manual mapping code in the `mapper/` package.

### Mapper declaration

```java
// mapper/OrderMapper.java
@Mapper(componentModel = "spring")            // Spring-managed @Component
public interface OrderMapper {

    // Generated model → JPA entity
    Order toEntity(CreateOrderRequest request);

    // JPA entity → generated response model
    OrderResponse toResponse(Order order);

    // If field names differ, add explicit @Mapping
    @Mapping(source = "customerId", target = "customer.id")
    Order toEntityWithCustomer(CreateOrderRequest request);
}
```

MapStruct picks up the interface and generates `OrderMapperImpl` automatically during
`mvn compile` / `./gradlew compileJava`. Inject the interface, not the impl:

```java
private final OrderMapper orderMapper;   // Spring injects OrderMapperImpl
```

### MapStruct rules

- Always declare mappers as **interfaces** (never abstract classes unless you need
  custom logic — then use `@Mapping` + `default` methods first).
- Use `componentModel = "spring"` so Spring manages lifecycle.
- Use `@Mapping(source=..., target=...)` when field names differ.
- For complex conversions add a `default` method in the mapper interface.
- **Never write `new OrderMapperImpl()`** — always inject via Spring.
- Mapper interfaces live in `mapper/` (one mapper class per aggregate/entity).
- Test mappers with plain unit tests — no Spring context needed.

## Dependency Direction (STRICT)

```
controller/   →  depends on  →  service/ (and dto/)
service/      →  depends on  →  repository/ (and model/, dto/, mapper/)
repository/   →  depends on  →  model/
```

- Controllers **NEVER** inject repositories directly — always go through a service
- Services **NEVER** return JPA entities to controllers — always map to DTOs
- Repositories contain **NO** business logic — only queries

## Controller Layer Rules

- Annotated with `@RestController` and `@RequestMapping`
- Inject service interfaces (or classes), NEVER repositories
- Validate input with `@Valid` on request DTOs
- Map to/from DTOs at this layer (or delegate to mapper)
- Return appropriate HTTP status codes (`@ResponseStatus`, `ResponseEntity`)
- No business logic — delegate everything to the service layer
- No `@Transactional` — that belongs in services
- Keep controllers thin: validate → delegate → respond

## Service Layer Rules

- Annotated with `@Service`
- All business rules and orchestration live here
- Use `@Transactional` for write operations (class-level or method-level)
- Accept DTOs or domain-specific commands as input
- Return DTOs or domain-specific responses (never raw entities to controllers)
- Throw domain-specific exceptions (not generic RuntimeException)
- Use constructor injection (no `@Autowired` on fields)
- One service per aggregate/entity is the default; split if complexity grows

## Repository Layer Rules

- Extend `JpaRepository<Entity, IdType>` or `CrudRepository`
- Use derived query methods when possible (`findByStatusAndCustomerId`)
- Use `@Query` with JPQL for complex queries
- Never put business logic in custom repository implementations
- Name custom query methods descriptively

## Entity / Model Rules

- Annotated with `@Entity`, `@Table`, `@Column`, etc.
- Use `@Id` with `@GeneratedValue` for primary keys
- Entities may contain simple validation logic (invariants), but complex
  business rules belong in the service layer
- Prefer `Long` or `UUID` for ID types
- Use `@CreatedDate`, `@LastModifiedDate` for auditing (with `@EntityListeners`)
- Relationships: prefer `@ManyToOne` (owning side); use `@OneToMany(mappedBy=...)` carefully
- Always define `equals()` and `hashCode()` based on the business key or ID

## DTO Rules

- **Prefer generated DTOs from the OpenAPI spec** (`com.company.app.api.model.*`).
  Only create hand-written DTOs in `dto/` when a model cannot be expressed in OpenAPI
  (e.g., internal service-to-service DTOs not part of the public API).
- Generated model classes are Java classes (not records) built by the openapi-generator.
  They carry Jakarta Validation annotations (`@NotNull`, `@NotBlank`, etc.) from the spec.
- Naming in the spec: use `CreateOrderRequest` / `OrderResponse` as schema names — the
  generator preserves these as class names.
- DTOs are **never** JPA entities — always separate classes.
- Never edit generated model classes — edit the spec instead.

## Error Handling

- Custom exceptions extend `RuntimeException` with meaningful messages
- `GlobalExceptionHandler extends ResponseEntityExceptionHandler` in `exception/GlobalExceptionHandler.java`
- Returns **RFC 9457 Problem Details** (`ProblemDetail` — Spring 6+ built-in)
- Enable: `spring.mvc.problemdetails.enabled=true` in `application.yml`
- Never expose stack traces externally — log with MDC correlation ID server-side
- Never catch generic `Exception` — catch specific types
- Common exceptions: `{Entity}NotFoundException`, `{Entity}AlreadyExistsException`,
  `BusinessRuleViolationException`

## Testing

- **Services**: JUnit 5 + Mockito (`@Tag("unit")`) — mock repositories and external dependencies
- **Controllers**: `@WebMvcTest` + `@MockBean OrderService` (`@Tag("unit")`)
- **Repositories**: `@DataJpaTest` + Testcontainers (`@Tag("integration")`)
- **External HTTP services**: `@WireMockTest` + stub patterns (`@Tag("integration")`)
- **Integration**: `@SpringBootTest` + Testcontainers (`@Tag("integration")`)
- **Architecture tests (optional)**: ArchUnit — uncomment `archunit-junit5` in build file; arch tests in `src/test/.../arch/`
- Test naming: `should_{expectedBehavior}_when_{condition}()`
- Coverage target: 80%+ on service layer, 90%+ on utilities
- JaCoCo excludes generated code (`**/api/**`, `**/*MapperImpl*`)
- Run unit tests only: `./mvnw test -Dgroups=unit`
- Run integration tests only: `./mvnw test -Dgroups=integration`

## Modern Java Best Practices

- Use `Objects.requireNonNull()` in constructors and method parameters for early null detection
- Use `Optional<T>` for return types that may have no value — never return `null`
- Use `toList()` on streams instead of `collect(Collectors.toList())` (Java 16+)
- Never use inner classes or inner records — each class/record must live in its own file
- Prefer `NullPointerException` with descriptive messages over silent null propagation
- Use pattern matching for `switch` with guards: `case Order o when o.isExpired() ->`
- Use record patterns for destructuring: `if (obj instanceof Order(var id, var status))`
- Use sequenced collection methods: `list.getFirst()`, `list.getLast()`, `list.reversed()`
- Use unnamed patterns for ignored components: `case OrderCreated(var id, _) ->`
- Prefer `ScopedValue` over `ThreadLocal` for request-scoped data with virtual threads
- Use stream gatherers for custom intermediate operations: `.gather(Gatherers.windowFixed(10))`

## Lombok (Optional)

Lombok is an optional dependency — see the commented block in `pom.xml` / `build.gradle.kts`.

**Allowed:** `@Slf4j`, `@RequiredArgsConstructor`, `@Builder`, `@Value`
**NOT allowed:** `@Data` on JPA entities (breaks JPA identity semantics)

To remove Lombok: delete the dependency and annotation processor entries from the build file, then replace usages with pure Java equivalents (records for DTOs/VOs, explicit constructors, `LoggerFactory.getLogger(...)`).

## ArchUnit (Optional)

ArchUnit can automatically enforce layered architecture rules as failing tests. To enable: uncomment `archunit-junit5` in the build file and add `src/test/java/.../arch/ArchitectureTest.java`. See `docs/TESTING.md` for example arch rules. Also update `docs/adr/0005-no-archunit.md`.

## Database Migrations

This project supports **both Flyway and Liquibase**. Detect which one is active by checking:
- **Flyway**: `src/main/resources/db/migration/` contains `V*__.sql` files
- **Liquibase**: `src/main/resources/db/changelog/` contains `db.changelog-master.yaml`

Also check the build file — only one migration dependency should be uncommented.

> **Choosing a migration tool:** When starting a new project from this template, pick ONE
> and remove the other. See `docs/adr/0004-migration-tool-choice.md` for guidance.

### Flyway (SQL-based)
- Migration files: `src/main/resources/db/migration/V{number}__{description}.sql`
- Spring config: `spring.flyway.enabled=true`
- Tracks history in `flyway_schema_history` table

### Liquibase (changelog-based)
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Changesets in: `src/main/resources/db/changelog/changes/`
- Naming: `{number}-{description}.yaml` (e.g., `001-create-orders-table.yaml`)
- Spring config: `spring.liquibase.enabled=true`
- Tracks history in `DATABASECHANGELOG` table

### Shared Rule
**Never use `spring.jpa.hibernate.ddl-auto=update` in production.** Schema changes must
always go through versioned migrations, regardless of which tool is used.

## Build Tool

This project ships with **both Maven and Gradle** configurations. Use whichever your team
prefers. Check which wrapper exists in your project root to know which is active:
- Maven: `mvnw` / `mvnw.cmd` + `pom.xml`
- Gradle: `gradlew` / `gradlew.bat` + `build.gradle.kts`

> **Choosing a build tool:** When starting a new project from this template, pick ONE and
> delete the other's files. See `docs/adr/0006-build-tool-choice.md` for guidance.

## Build & Run

### Maven
```bash
./mvnw clean install              # Build
./mvnw spring-boot:run            # Run locally
./mvnw test                       # All tests
./mvnw test -Dgroups=unit         # Unit tests only
./mvnw test -Dgroups=integration  # Integration tests only
./mvnw test jacoco:report         # Tests with coverage
```

### Gradle
```bash
./gradlew build                   # Build
./gradlew bootRun                 # Run locally
./gradlew test                    # All tests
./gradlew test -Dgroups=unit      # Unit tests only
./gradlew test -Dgroups=integration # Integration tests only
./gradlew jacocoTestReport        # Tests with coverage
```

## Git Workflow

- Commit format: `type(scope): description` (e.g., `feat(orders): add cancel endpoint`)
- PR size: < 400 lines of production code
- Every PR must include tests
- Document architectural decisions in `docs/adr/`

## Common Tasks

### New Feature / Endpoint

1. **Edit the spec first**: add the operation (path, method, request/response schemas) to
   `src/main/resources/openapi/openapi.yaml`
2. **Regenerate**: run `./mvnw generate-sources` (or `./gradlew openApiGenerate`) to
   produce the updated controller interface and model classes
3. Create or update JPA entity in `model/` if needed
4. Add repository method in `repository/` if a new query is needed
5. Implement business logic in `service/` (new method or new service class)
6. Add or update MapStruct method in `mapper/` for generated model ↔ entity conversion
7. Add endpoint in `controller/` — make the controller implement the generated API
   interface and override the new method
8. Add database migration if schema changed
9. Write tests: service unit test + controller MockMvc test + integration test
10. Record rationale in `docs/adr/` if the decision is significant

### New External Integration (e.g., payment provider)

1. Create a service interface (e.g., `PaymentService`) in `service/`
2. Create the implementation (e.g., `StripePaymentServiceImpl`) in `service/` or a
   dedicated `integration/` package
3. Inject the interface into consuming services
4. Add `@Configuration` for client setup in `config/`
5. Write integration tests with WireMock or Testcontainers

### New Entity

1. Add schemas and paths for the entity to `src/main/resources/openapi/openapi.yaml`
2. Regenerate sources: `./mvnw generate-sources`
3. Create JPA entity in `model/` with proper annotations
4. Create Spring Data repository in `repository/`
5. Add MapStruct mapper in `mapper/` (generated model ↔ JPA entity)
6. Create service in `service/`
7. Add database migration (see "Database Migrations" section)
8. Create controller in `controller/` implementing the generated API interface

## What NOT To Do

- Do NOT put business logic in controllers — delegate to services
- Do NOT inject repositories into controllers — always go through services
- Do NOT return JPA entities from controllers — always use generated model DTOs
- Do NOT use `@Autowired` on fields — use constructor injection
- Do NOT skip `@Transactional` on service methods that write data
- Do NOT catch generic `Exception` — catch specific types
- Do NOT use `System.out.println` — use SLF4J logger
- Do NOT use `spring.jpa.hibernate.ddl-auto=update` in production
- Do NOT add new dependencies without explicit mention
- Do NOT put query logic in services — use repository methods or `@Query`
- Do NOT use field injection (`@Autowired` on private fields)
- Do NOT use inner classes or inner records — extract them to their own files
- Do NOT return `null` from methods — use `Optional` instead
- Do NOT use `collect(Collectors.toList())` — use `toList()` instead
- Do NOT leave constructor parameters unchecked — use `Objects.requireNonNull()`
- Do NOT edit files under `target/generated-sources/` or `build/generated/` — edit the spec
- Do NOT write manual mapper code when a MapStruct `@Mapping` annotation is sufficient
- Do NOT use ModelMapper or hand-written mapping — use MapStruct interfaces only
- Do NOT put OpenAPI spec changes in a separate PR from the code that implements them
- Do NOT use `and()` in Spring Security configuration — removed in Spring Boot 3.5.14 / Security 7; use Lambda DSL
- Do NOT store long-lived state in `ThreadLocal` with virtual threads — use `ScopedValue` or Spring's `RequestContextHolder`
- Do NOT use `@Data` on JPA entities — breaks JPA identity semantics; use explicit `equals`/`hashCode` on business key
