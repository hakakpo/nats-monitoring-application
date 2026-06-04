# ADR-0007: Contract-First API Development with OpenAPI Generator

## Status

accepted

## Context

The project needs a clear, stable contract between the REST API layer and its consumers
(front-end, mobile, other services). Without a contract-first approach, the API contract
is implicitly defined by whatever the controller happens to return, which leads to:

- **API drift**: changes to internal classes accidentally break external consumers
- **Undocumented contracts**: consumers must inspect source code or run the app to know
  what the API expects and returns
- **Inconsistent validation**: validation constraints are scattered across controller
  method signatures and are easy to miss or duplicate
- **Controller bloat**: controllers carry `@RequestBody`, `@PathVariable`, `@Valid`, and
  swagger annotation noise alongside actual logic

A contract-first approach inverts this: define the API spec first, generate the
controller interface from it, and implement only the business delegation in the controller.

### Technology options considered

| Option | Pros | Cons |
|---|---|---|
| **openapi-generator-maven-plugin (spring generator)** | Industry standard, generates Jakarta-native interfaces, active community | Generated code must be kept out of git |
| springdoc code-first (@Operation annotations) | Less setup, spec auto-generated from annotations | Spec is an artifact, not a source — consumers can't review/pin it; annotations pollute controllers |
| Manual DTOs + hand-written swagger docs | Zero tooling | Spec drifts from implementation immediately; duplicated effort |

### Version compatibility

The `openapi-generator-maven-plugin` and Gradle plugin version **7.12.0** is used.

Key configuration flags for Spring Boot 4 / Java 21:

- `useSpringBoot3=true` — generates `jakarta.*` imports (not `javax.*`). This flag covers
  Spring Boot 3.x **and** Spring Boot 4.x because both use the Jakarta EE namespace.
- `useJakartaEe=true` — Jakarta EE 10 annotations on generated models
- `interfaceOnly=true` — generates a `@RequestMapping` interface only; your controller
  provides the implementation
- `openApiNullable=false` — avoids adding `jackson-databind-nullable` as a runtime dependency

These settings are compatible with Java 21's standard annotation processing. No special
compiler flags are needed for the generated code to compile under Java 21.

## Decision

Adopt **contract-first API development** using `openapi-generator-maven-plugin` (Maven)
and the `org.openapi.generator` Gradle plugin.

### Workflow

1. The API spec lives at `src/main/resources/openapi/openapi.yaml` (OpenAPI 3.1.0).
2. Running `./mvnw generate-sources` (or `./gradlew openApiGenerate`) generates:
   - One `*Api` interface per OpenAPI **tag** (e.g., `OrdersApi`) → package `com.company.app.api`
   - Request/response model classes → package `com.company.app.api.model`
3. Controllers in `controller/` **implement** the generated interface.
4. The generated sources are placed in `target/generated-sources/openapi/` (Maven) or
   `build/generated/sources/openapi/` (Gradle) and are **not committed to git**.

### What is generated vs. what is hand-written

| Artifact | Source |
|---|---|
| `OrdersApi` interface | Generated from spec — never edit |
| `CreateOrderRequest`, `OrderResponse` models | Generated from spec — never edit |
| `OrderController implements OrdersApi` | Hand-written |
| `OrderMapper` (MapStruct) | Hand-written — maps generated model ↔ JPA entity |
| `OrderService` | Hand-written — contains business logic |

## Consequences

### Positive

- **API as contract**: the `openapi.yaml` file is reviewable, versionable, and pinnable
  by consumers before implementation begins
- **Compile-time safety**: if the spec changes (new required field, renamed parameter),
  the controller fails to compile until updated — no runtime surprises
- **Reduced controller noise**: controllers implement the generated interface and focus
  only on delegation to the service layer — no `@Operation`, `@ApiResponse` annotations
  needed in hand-written code
- **Single regeneration step**: `./mvnw generate-sources` updates all interfaces and models
  after a spec change
- **Jakarta namespace**: the `useSpringBoot3=true` flag produces code compatible with
  Spring Boot 3.x and 4.x (both use `jakarta.*`)

### Negative (Trade-offs)

- **Build step required**: generated sources must exist before `compileJava` runs —
  a clean checkout must run `generate-sources` first (handled automatically by the plugin
  binding to the `generate-sources` Maven lifecycle phase)
- **IDE configuration**: IDEs must be told about the generated source root
  (`target/generated-sources/openapi/src/main/java`) to resolve imports correctly
- **Spec maintenance**: any API change requires editing the spec AND regenerating, not
  just modifying a controller method signature
- **No springdoc for Spring Boot 4 yet**: springdoc-openapi 2.x targets Spring Boot 3.x.
  The Swagger UI dependency is commented out in `pom.xml` / `build.gradle.kts` until a
  compatible version for Spring Boot 4 is confirmed at https://springdoc.org

## Alternatives Considered

### Alternative 1: Code-First with springdoc @Operation Annotations

Write controllers normally, annotate with `@Operation`, `@ApiResponse`, and let springdoc
auto-generate the spec at runtime.

**Rejected because**: the spec is an output artifact, not a source artifact. Consumers
cannot review or pin a spec that doesn't exist until the application starts. Annotations
mix documentation with controller code and create maintenance overhead.

### Alternative 2: Manual DTOs, No Generator

Write all request/response DTOs by hand and document them separately.

**Rejected because**: spec and implementation drift immediately. Any refactoring must be
reflected in documentation manually. No compile-time contract enforcement.

---

## Changelog

| Date | Event |
|------|-------|
| 2026-03-27 | Decision accepted and implemented in project configuration |
