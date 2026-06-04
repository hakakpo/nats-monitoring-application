# Java Coding Conventions

## Editor & Formatting
- `.editorconfig` is the source of truth for editor formatting
- Java files (`*.java`) use 4-space indentation
- XML/YAML/properties/JSON files use 2-space indentation
- Line endings are `lf` and files must end with a final newline
- Do not manually reformat outside these rules in mixed-change commits

## Language & Version
- Java 21+ (LTS) with preview features disabled in production
- Use records for DTOs and value objects
- Use sealed interfaces/classes where appropriate
- Prefer `var` for local variables when the type is obvious from the RHS
- Use `Objects.requireNonNull()` for constructor and method parameter null checking
- Return `Optional<T>` instead of null for values that may be absent
- Use `toList()` instead of `collect(Collectors.toList())`
- One class/record per file — no inner classes or inner records

## Java 21/25 Modern Features

### Virtual Threads (Java 21+ — Project Loom)
Enable in Spring Boot: add `spring.threads.virtual.enabled=true` to `application.yml` (SB 3.2+; confirm default-on in your SB4 release). Virtual threads are lightweight, JVM-managed — millions can run concurrently without OS thread overhead.

```java
// No code change needed — Spring wires virtual threads automatically.
// application.yml:
// spring:
//   threads:
//     virtual:
//       enabled: true
```

**Rules:**
- Use for I/O-bound work — database calls, HTTP calls, file I/O all benefit automatically
- Do NOT store long-lived state in `ThreadLocal` — prefer `ScopedValue` (Java 21+) for request-scoped data
- JDBC blocking is fine — virtual threads park instead of blocking an OS thread
- `@Async` methods use virtual threads automatically when enabled

### Pattern Matching for Switch (Java 21+)
```java
String describe(Object obj) {
    return switch (obj) {
        case Order o when o.isExpired() -> "expired order " + o.id();
        case Order o                    -> "active order " + o.id();
        case null                       -> "null";
        default                         -> "unknown";
    };
}
```

### Record Patterns (Java 21+)
```java
// Destructure records inline in conditions
if (result instanceof OrderResult(var orderId, var status)) {
    log.atInfo().addKeyValue("orderId", orderId).log("Status: {}", status);
}
```

### Sequenced Collections (Java 21+)
```java
var first = items.getFirst();   // replaces: items.get(0)
var last  = items.getLast();    // replaces: items.get(items.size() - 1)
var rev   = items.reversed();   // new: reversed view without copying
```

### Unnamed Patterns and Variables (Java 22+)
```java
switch (event) {
    case OrderCreated(var id, _) -> handleCreated(id);   // ignore second component
    case OrderCancelled _        -> handleCancelled();   // ignore entire payload
}
```

### Stream Gatherers (Java 24+)
```java
// Custom intermediate stream operations
var windows = orders.stream()
    .gather(Gatherers.windowFixed(10))
    .toList();
```

## Spring Boot 3.5.x

Compatible with Java 21 and Java 21.

**Key features:**
- **`@Fallback`** — define fallback beans without `@ConditionalOnMissingBean`:
  ```java
  @Fallback   // used only when no other PaymentService bean is defined
  @Component
  public class StubPaymentService implements PaymentService { ... }
  ```
- **CDS (Class Data Sharing):** Faster startup via AOT processing. Use `spring.context.exit=onRefresh` in CI to train the CDS archive.
- Fully compatible with the Spring Boot 3.x library ecosystem.

## Spring Boot 4.x

Baseline: **Spring Framework 7**, **Java 21 minimum**.

### Virtual Threads
Spring Boot 4.x enables virtual threads by default. For explicit control:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Spring Security 7 — Lambda DSL is Mandatory
The old method-chaining API (`http.authorizeRequests().and().csrf()...`) is **removed in SB4**.
Use the Lambda DSL for all security configuration:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
}
```

### RFC 9457 Problem Details (Spring 6+)
Enable built-in Problem Details:
```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

## Naming
- **Packages**: `com.company.module.submodule` (lowercase, no underscores)
- **Classes/Interfaces**: PascalCase (`OrderService`, `PaymentGateway`)
- **Methods/Variables**: camelCase (`calculateTotal`, `orderCount`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **Test classes**: `{ClassName}Test` (unit), `{ClassName}IT` (integration)

## Project Structure (Layered)
```
src/
├── main/java/com/company/app/
│   ├── controller/        # @RestController — HTTP concerns only
│   ├── service/           # @Service — business logic, @Transactional
│   ├── repository/        # @Repository — Spring Data JPA interfaces
│   ├── model/             # @Entity — JPA entities and enums
│   ├── dto/               # Request/response records with @Valid
│   ├── mapper/            # DTO ↔ entity converters
│   ├── exception/         # Custom exceptions + @RestControllerAdvice
│   ├── config/            # @Configuration classes
│   └── event/             # Application events (optional)
├── main/resources/
│   ├── application.yml
│   ├── application-{profile}.yml
│   └── db/                # Migrations (Flyway: db/migration/ OR Liquibase: db/changelog/)
└── test/java/com/company/app/
    ├── controller/        # @WebMvcTest (HTTP layer only)
    ├── service/           # Unit tests (Mockito)
    ├── repository/        # @DataJpaTest (query tests)
    └── integration/       # @SpringBootTest (full stack)
```

## Architecture Rules (Layered)
1. **Business logic in services**: Controllers and repositories contain no business rules.
2. **Dependency direction**: controller → service → repository. Never skip a layer.
3. **DTOs at the boundary**: Controllers accept/return DTOs, never JPA entities.
4. **Constructor injection**: No `@Autowired` on fields — always constructor injection.
5. **Transactional services**: Write operations use `@Transactional`. Read-only with `readOnly = true`.
6. **One responsibility per class**: Controllers don't validate business rules. Repos don't orchestrate.
7. **Mappers between layers**: Explicit mapping between DTOs and entities (MapStruct or manual).

## Error Handling

Use **RFC 9457 Problem Details** (`ProblemDetail` — built into Spring 6+). Enable with `spring.mvc.problemdetails.enabled=true`.

### Global Exception Handler Pattern
```java
// exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://api.company.com/errors/order-not-found"));
        problem.setTitle("Order Not Found");
        problem.setProperty("orderId", ex.getOrderId());
        return problem;
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
            ex.getErrorCode().getStatus(), ex.getMessage());
        problem.setType(URI.create(
            "https://api.company.com/errors/" + ex.getErrorCode().name().toLowerCase()));
        problem.setTitle(ex.getErrorCode().getTitle());
        return problem;
    }
}
```

**Rules:**
- `GlobalExceptionHandler extends ResponseEntityExceptionHandler` to also handle Spring MVC exceptions
- Never expose stack traces in production — log server-side with correlation ID, return only `ProblemDetail`
- Never catch generic `Exception` — catch specific types
- Log at ERROR level before mapping: `log.atError().addKeyValue("orderId", id).setCause(ex).log("Order not found")`

## Logging

### SLF4J 2.x Fluent API (preferred over string concatenation)
```java
// Structured key-value logging — preferred for observability pipelines
log.atInfo()
   .addKeyValue("orderId", orderId)
   .addKeyValue("amount", amount)
   .log("Order processed");

// With exception
log.atError()
   .addKeyValue("orderId", orderId)
   .setCause(ex)
   .log("Failed to process order");

// Old style still valid (keep consistent within a class)
log.info("Order processed: orderId={}, amount={}", orderId, amount);
```

### MDC Correlation (set in filter)
```java
// config/CorrelationIdFilter.java
MDC.put("requestId", Optional.ofNullable(request.getHeader("X-Request-Id"))
    .orElse(UUID.randomUUID().toString()));
try {
    filterChain.doFilter(request, response);
} finally {
    MDC.clear();   // always remove to prevent ThreadLocal leaks
}
```

### Log Levels
| Level | Use for | Environments |
|-------|---------|-------------|
| ERROR | Unrecoverable failures, exceptions | All |
| WARN  | Recoverable issues, degraded state | All |
| INFO  | Business events (order created, payment processed) | All |
| DEBUG | Request details, intermediate state | Local/Dev only |
| TRACE | SQL, full payloads | Local only |

### Structured JSON Logging (Production — optional)
Add `logstash-logback-encoder` to `pom.xml` and configure `logback-spring.xml` with `LogstashEncoder` for the `prod` profile. JSON logs are required for log aggregation systems (ELK, Loki, Datadog).

### Service Layer Rule
Keep logging in service and controller layers. Repository methods should not log business events — they only log infrastructure concerns (query errors).

### What NOT to Log
- Passwords, tokens, API keys, PII (names, emails, national IDs)
- Full request/response bodies in production
- Stack traces in HTTP responses (log server-side, return `ProblemDetail`)

## Lombok (Optional)

Lombok is an optional dependency. Your team may choose to use it or remove it entirely.

**Allowed Lombok annotations:**
- `@Slf4j` — logger field: `private static final Logger log = LoggerFactory.getLogger(ClassName.class)`
- `@RequiredArgsConstructor` — constructor for `final` fields
- `@Builder` — builder pattern on requests and responses
- `@Value` — immutable value objects (records are preferred in Java 21+)

**NOT allowed:**
- `@Data` on JPA entities — generates `equals`/`hashCode` using all fields, breaks JPA identity semantics
- `@EqualsAndHashCode` with `callSuper=false` on entities — silent bugs with inheritance

**Pure Java equivalents (use if Lombok is removed):**
```java
// @Slf4j equivalent
private static final Logger log = LoggerFactory.getLogger(OrderService.class);

// @RequiredArgsConstructor equivalent — constructor injection
public OrderService(OrderRepository orderRepository, CustomerService customerService) {
    this.orderRepository  = Objects.requireNonNull(orderRepository,  "orderRepository must not be null");
    this.customerService  = Objects.requireNonNull(customerService,  "customerService must not be null");
}
```

To remove Lombok: delete the `<dependency>` and annotation processor paths from `pom.xml` (or the plugin from `build.gradle.kts`) and replace usages with the pure Java equivalents above.

## Testing
- **Unit tests**: JUnit 5 + Mockito. Test service logic in isolation.
- **Controller tests**: `@WebMvcTest` + MockMvc. Test HTTP layer in isolation.
- **Repository tests**: `@DataJpaTest` + Testcontainers. Test queries against real DB.
- **Integration tests**: `@SpringBootTest` + Testcontainers for full stack
- **Coverage target**: 80% line coverage on service layer
- **Test naming**: `should_returnOrder_when_validIdProvided()`
- **No test logic in production code**: No `@Profile("test")` hacks

## Dependencies & Libraries
- **Spring Boot 4.x** as the base framework
- **openapi-generator-maven-plugin / org.openapi.generator Gradle plugin** (v7.12.0+) for
  contract-first controller interface and model generation
- **MapStruct** (v1.6.3+) for object mapping — generated at compile time from `@Mapper` interfaces.
  Never use ModelMapper or hand-written mapping code.
- **swagger-annotations** (v2.2.28+) — required at compile time by generated interfaces
- **springdoc-openapi-starter-webmvc-ui** — optional Swagger UI; check https://springdoc.org
  for the version compatible with Spring Boot 4.x before enabling
- **Lombok** — allowed for `@Slf4j`, `@Builder`, `@RequiredArgsConstructor` only
- **Jakarta Validation** for input validation (`@Valid`, `@NotNull`, etc.)
- **Flyway** or **Liquibase** for database migrations (never Hibernate auto-DDL in production)
- **Jackson** for JSON (configure globally, never per-endpoint)

## API Design (Contract-First with OpenAPI)

The REST API contract is defined in `src/main/resources/openapi/openapi.yaml` and generated
into controller interfaces and model classes. Follow these conventions in the spec:

- **OpenAPI version**: use 3.1.0
- **One tag per resource**: `orders`, `customers`, `products` — each tag → one generated interface
- **Schema naming**: `PascalCase` for schemas (becomes the Java class name exactly)
  - Requests: `CreateOrderRequest`, `UpdateOrderRequest`
  - Responses: `OrderResponse`, `OrderListResponse`
  - Errors: `ProblemDetail` (RFC 9457)
- **Operation IDs**: `camelCase` verb + noun matching the Java method name (`createOrder`,
  `getOrderById`, `listOrders`, `cancelOrder`)
- **Path conventions**:
  - Collections: `/api/v1/orders`
  - Single resource: `/api/v1/orders/{orderId}`
  - Sub-resources: `/api/v1/orders/{orderId}/items`
- **HTTP status codes** must be explicit in the spec:
  - `200` for GET, `201` for POST (create), `204` for DELETE
  - `400` for validation errors, `404` for not found, `409` for conflicts
- **Versioning** in URL path (`/api/v1/`)
- **Pagination parameters** modelled with `page`, `size`, `sort` query params
- **Never put business rules in the spec** — just types and constraints (`minLength`, `pattern`)
- **Use `$ref`** to reuse schemas — avoid copy-paste duplication in the spec

## MapStruct Conventions

```java
// mapper/OrderMapper.java
@Mapper(componentModel = "spring")      // Spring-managed bean
public interface OrderMapper {

    Order toEntity(CreateOrderRequest request);  // generated model → JPA entity
    OrderResponse toResponse(Order order);        // JPA entity → generated model

    // Explicit field mapping when names differ
    @Mapping(source = "status.displayName", target = "statusLabel")
    OrderResponse toResponseWithLabel(Order order);

    // Use default methods for complex transformations
    default String formatOrderId(Long id) {
        return "ORD-" + id;
    }
}
```

- One mapper interface per aggregate/entity (`OrderMapper`, `CustomerMapper`)
- Always use `componentModel = "spring"` — never instantiate mappers manually
- Prefer `@Mapping` annotations over `default` method overrides when possible
- `default` methods are allowed for value conversions (e.g., enum → String)
- Never import MapStruct in service or repository classes — mappers are injected where needed
- Tests for mappers are plain unit tests (no Spring context needed)

## Security

**Requires** `spring-boot-starter-security` — see commented dependency in `pom.xml` / `build.gradle.kts`.

### Lambda DSL (Mandatory in Spring Boot 4.x)
The old chained API (`http.authorizeRequests().and().csrf()`) is **removed** in Spring Security 7 / SB4.

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .csrf(AbstractHttpConfigurer::disable)              // stateless REST API
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }

    @Bean
    CorsConfigurationSource corsSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://app.company.com"));  // explicit, never "*"
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

### Method-Level Security
```java
// controller/OrderController.java — controllers or services, depending on context
@PreAuthorize("hasRole('MANAGER') or hasAuthority('SCOPE_orders:write')")
public ResponseEntity<OrderResponse> createOrder(@Valid CreateOrderRequest request) { ... }
```

### Rules
- Never hardcode secrets — use environment variables or a vault (`spring.config.import=vault:...`)
- CORS: explicit allowed origins, never `"*"` in production
- `@PreAuthorize` on controllers (HTTP access control) or services (business rule authorization)
- SQL injection: use JPA parameterized queries — never string concatenation in `@Query`

## Performance
- Use `@Cacheable` for expensive read-heavy operations
- Lazy loading by default for JPA associations
- Use `@EntityGraph` or `JOIN FETCH` for known eager-load scenarios
- Paginate all list endpoints
- Use connection pooling (HikariCP — Spring Boot default)

## Git & Code Review
- Commit messages: `type(scope): description` (e.g., `feat(orders): add bulk import endpoint`)
- PR size: < 400 lines of production code
- Every PR must have tests
- No `@SuppressWarnings` without a comment explaining why
