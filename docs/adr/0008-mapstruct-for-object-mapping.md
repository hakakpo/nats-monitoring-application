# ADR-0008: MapStruct for Object Mapping

## Status

accepted

## Context

The layered architecture requires explicit conversion between:

1. **Generated request models → JPA entities** (e.g., `CreateOrderRequest → Order`)
2. **JPA entities → generated response models** (e.g., `Order → OrderResponse`)

Without a dedicated mapping strategy, teams typically write one of:

| Approach | Problem |
|---|---|
| Hand-written mapper classes | Verbose, error-prone, breaks silently when fields are added |
| ModelMapper (reflection-based) | No compile-time safety, poor performance, opaque failures |
| `BeanUtils.copyProperties` | Shallow copy, no type conversion, breaks on rename |
| Jackson serialisation round-trip | Terrible performance, abuses a JSON library |

The project already mentions MapStruct as the preferred approach in `CONVENTIONS.md`;
this ADR formalises the decision and documents configuration details.

## Decision

Use **MapStruct 1.6.3** as the sole mapping library. All object mapping between
DTOs/generated models and JPA entities is done through MapStruct `@Mapper` interfaces.

### Why MapStruct

- **Compile-time code generation**: `mapstruct-processor` generates `*MapperImpl` classes
  during `mvn compile`. Mapping errors (missing fields, type mismatches) are caught at
  compile time, not at runtime.
- **No reflection at runtime**: generated code is plain Java method calls — fast and
  debuggable with a stack trace.
- **Spring integration**: `componentModel = "spring"` makes the generated impl a Spring
  `@Component`, injected like any other bean.
- **Java 21 compatible**: the annotation processor runs on the standard `javac` annotation
  processing API and is compatible with Java 21.
- **Incremental updates**: adding a field to an entity requires adding one `@Mapping`
  annotation — not rewriting a method.

### Configuration

**Maven** (`pom.xml`):
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
<!-- annotation processor wired via maven-compiler-plugin annotationProcessorPaths -->
```

**Gradle** (`build.gradle.kts`):
```kotlin
implementation("org.mapstruct:mapstruct:1.6.3")
annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
```

### Standard mapper pattern

```java
// mapper/OrderMapper.java
@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toEntity(CreateOrderRequest request);   // generated model → entity
    OrderResponse toResponse(Order order);         // entity → generated model

    @Mapping(source = "status.name", target = "statusLabel")
    OrderResponse toResponseWithLabel(Order order);
}
```

### Rules

1. One mapper interface per aggregate (e.g., `OrderMapper`, `CustomerMapper`).
2. Declare mappers as **interfaces** (not abstract classes) unless custom logic is needed —
   use `default` methods in the interface for that.
3. Always use `componentModel = "spring"` — never instantiate `new OrderMapperImpl()`.
4. Use `@Mapping(source = ..., target = ...)` for field-name differences.
5. Use `@BeanMapping(ignoreByDefault = true)` for explicit partial mapping (e.g., PATCH).
6. Mappers live in `mapper/` only — services inject the interface, not the impl.
7. Test mappers with plain unit tests (construct `OrderMapperImpl` directly — no Spring needed).

## Consequences

### Positive

- **Compile-time contract**: renaming a JPA entity field causes a MapStruct compile error
  if the mapper is not updated — no silent data loss
- **Readable generated code**: `OrderMapperImpl.java` is plain Java, readable in the IDE,
  debuggable with breakpoints
- **No runtime reflection**: zero overhead from reflection-based mapping frameworks
- **Replaces the `dto/` package for generated models**: since the OpenAPI generator
  produces the request/response model classes, MapStruct maps directly from/to those
  generated classes — no separate hand-written DTO layer is needed for standard API operations

### Negative (Trade-offs)

- **Annotation processor in build**: IDEs and build tools must be configured to run
  the annotation processor (`mapstruct-processor`) — covered by the `pom.xml`/`build.gradle.kts`
  configuration in this project
- **Circular dependency risk with Lombok**: if Lombok is also used, the annotation
  processor order matters. In Maven, list `mapstruct-processor` after `lombok-mapstruct-binding`
  in `annotationProcessorPaths` if Lombok is added in future

## Alternatives Considered

### Alternative 1: ModelMapper

Reflection-based, requires no annotations. Rejected because: no compile-time safety,
difficult to debug when fields silently don't map, poor performance on high-throughput paths.

### Alternative 2: Hand-Written Mapper Classes

Simple Java classes with explicit field assignments. Rejected because: verbose, must be
kept in sync manually with every entity and spec change, and silently drops fields when
someone adds a new property and forgets the mapper.

---

## Changelog

| Date | Event |
|------|-------|
| 2026-03-27 | Decision accepted and implemented in project configuration |
