# Architecture Overview — Java Classic Layered Architecture

## Philosophy

This application follows a **Classic Layered Architecture**, the most widely adopted pattern
in Spring Boot applications. Each layer has a clear responsibility and depends only on the
layer directly below it. Spring framework conventions are fully embraced — annotations like
`@Service`, `@Repository`, and `@RestController` are used throughout.

The guiding principle: **separation of concerns**. Controllers handle HTTP, services handle
business logic, repositories handle persistence.

## Layered Architecture Diagram

```
                    ┌─────────────────────────────────┐
                    │         PRESENTATION             │
  HTTP Request ───▶ │  @RestController                 │
                    │  Validates input (@Valid)         │
                    │  Maps DTOs, returns HTTP status   │
                    └──────────────┬──────────────────┘
                                   │ calls
                                   ▼
                    ┌─────────────────────────────────┐
                    │         SERVICE (Business)       │
                    │  @Service + @Transactional        │
                    │  Business rules & orchestration   │
                    │  Converts between DTOs & entities │
                    └──────────────┬──────────────────┘
                                   │ calls
                                   ▼
                    ┌─────────────────────────────────┐
                    │         PERSISTENCE              │
                    │  @Repository (Spring Data JPA)   │
                    │  CRUD + custom queries            │
                    │  JPA entities                     │
                    └──────────────┬──────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────────┐
                    │         DATABASE                 │
                    │  Oracle 19.3c                     │
                    └─────────────────────────────────┘
```

## Package Structure

```
src/main/java/com/company/app/
│
├── controller/                      ← REST API layer
│   ├── OrderController.java         ← @RestController, handles HTTP
│   ├── CustomerController.java
│   └── ProductController.java
│
├── service/                         ← Business logic layer
│   ├── OrderService.java            ← @Service, business rules, @Transactional
│   ├── CustomerService.java
│   ├── ProductService.java
│   └── NotificationService.java     ← Cross-cutting business service
│
├── repository/                      ← Data access layer
│   ├── OrderRepository.java         ← extends JpaRepository<Order, Long>
│   ├── CustomerRepository.java
│   └── ProductRepository.java
│
├── model/                           ← JPA entities
│   ├── Order.java                   ← @Entity with JPA annotations
│   ├── OrderItem.java               ← @Entity (child)
│   ├── OrderStatus.java             ← Enum
│   ├── Customer.java
│   └── Product.java
│
├── dto/                             ← Request/response DTOs (records)
│   ├── CreateOrderRequest.java      ← record with @Valid annotations
│   ├── UpdateOrderRequest.java
│   ├── OrderResponse.java           ← record (plain, no validation)
│   ├── OrderListResponse.java
│   ├── CustomerResponse.java
│   └── ErrorResponse.java           ← RFC 9457 Problem Detail
│
├── mapper/                          ← DTO ↔ entity converters
│   ├── OrderMapper.java             ← MapStruct or manual mapping
│   └── CustomerMapper.java
│
├── exception/                       ← Error handling
│   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice
│   ├── OrderNotFoundException.java
│   ├── InsufficientStockException.java
│   ├── BusinessRuleViolationException.java
│   └── DuplicateEntityException.java
│
├── config/                          ← Spring configuration
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   ├── JacksonConfig.java
│   └── CacheConfig.java
│
├── event/                           ← Application events (optional)
│   ├── OrderCreatedEvent.java
│   └── OrderEventListener.java
│
└── Application.java                 ← @SpringBootApplication entry point

src/main/resources/
├── application.yml
├── application-dev.yml
├── application-staging.yml
├── application-prod.yml
└── db/
    └── changelog/                   ← Liquibase changelogs
        ├── db.changelog-master.yaml
        └── changes/

src/test/java/com/company/app/
├── controller/                      ← @WebMvcTest (HTTP layer tests)
├── service/                         ← Unit tests (Mockito)
├── repository/                      ← @DataJpaTest (query tests)
└── integration/                     ← @SpringBootTest (full stack)
```

## The Golden Rules

### 1. Business Logic Lives in Services

The service layer is the ONLY place for business rules. This means:

- **Controllers** validate HTTP input and delegate. No `if/else` business decisions.
- **Repositories** execute queries. No business calculations or rule checks.
- **Services** own the logic: validation, orchestration, state transitions, calculations.

### 2. Dependency Direction (STRICTLY ENFORCED)

```
controller/  →  depends on  →  service/ (and dto/)
service/     →  depends on  →  repository/ (and model/, dto/, mapper/)
repository/  →  depends on  →  model/
```

This means:
- Controllers NEVER import repository classes
- Repositories NEVER import controller or service classes
- Services are the bridge between presentation and persistence

### 3. DTOs at the API Boundary

Controllers accept and return **DTOs** (Data Transfer Objects), never JPA entities.
This prevents:
- Exposing internal DB schema to clients
- Lazy-loading issues (LazyInitializationException)
- Over-fetching sensitive fields
- Circular serialization issues

```java
// dto/CreateOrderRequest.java — what the client sends
public record CreateOrderRequest(
    @NotNull UUID customerId,
    @NotEmpty List<OrderItemRequest> items
) {}

// dto/OrderResponse.java — what the client receives
public record OrderResponse(
    Long id,
    UUID customerId,
    String status,
    BigDecimal total,
    LocalDateTime createdAt
) {}
```

### 4. Mapper Layer Converts Between DTOs and Entities

```java
// mapper/OrderMapper.java
@Component
public class OrderMapper {
    public Order toEntity(CreateOrderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var order = new Order();
        order.setCustomerId(request.customerId());
        order.setStatus(OrderStatus.CREATED);
        return order;
    }

    public OrderResponse toResponse(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getStatus().name(),
            order.getTotal(),
            order.getCreatedAt()
        );
    }
}
```

### 5. Services Use Constructor Injection

```java
// service/OrderService.java
@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, CustomerService customerService, OrderMapper orderMapper) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.customerService = Objects.requireNonNull(customerService, "customerService must not be null");
        this.orderMapper = Objects.requireNonNull(orderMapper, "orderMapper must not be null");
    }

    public OrderResponse createOrder(CreateOrderRequest request) {
        // Business rule: check credit limit
        customerService.validateCreditLimit(request.customerId());

        var order = orderMapper.toEntity(request);
        order.calculateTotal();

        var saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        var order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toResponse(order);
    }
}
```

### 6. Controllers Are Thin

```java
// controller/OrderController.java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping
    public Page<OrderResponse> list(Pageable pageable) {
        return orderService.listOrders(pageable);
    }
}
```

## Request Flow (Complete)

```
1. HTTP POST /api/v1/orders
2. → OrderController.create() — validates @Valid, delegates to service
3. → OrderService.createOrder() — checks business rules, maps DTO → entity
4.   → CustomerService.validateCreditLimit() — business rule check
5.   → Order.calculateTotal() — entity-level calculation
6.   → OrderRepository.save(order) — persists to DB
7. ← OrderMapper.toResponse(saved) — entity → DTO
8. ← Controller returns 201 Created with OrderResponse body
```

## Testing Strategy by Layer

### Service Layer (Unit Tests — Mockito)
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock CustomerService customerService;
    @Mock OrderMapper orderMapper;
    @InjectMocks OrderService orderService;

    @Test
    void should_createOrder_when_creditLimitNotExceeded() {
        var request = new CreateOrderRequest(customerId, items);
        when(orderMapper.toEntity(request)).thenReturn(testOrder);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        var result = orderService.createOrder(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(customerService).validateCreditLimit(customerId);
        verify(orderRepository).save(any());
    }
}
```

### Controller Layer (MockMvc — HTTP-only)
```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService;

    @Test
    void should_return201_when_orderCreated() throws Exception {
        when(orderService.createOrder(any())).thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(APPLICATION_JSON)
                .content(validRequestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

### Repository Layer (DataJpaTest — real DB)
```java
@DataJpaTest
@Testcontainers
class OrderRepositoryTest {
    @Container static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart");
    @Autowired OrderRepository orderRepository;

    @Test
    void should_findOrders_when_filterByStatus() {
        orderRepository.save(testOrder(OrderStatus.CREATED));
        var results = orderRepository.findByStatus(OrderStatus.CREATED);
        assertThat(results).hasSize(1);
    }
}
```

### Integration Tests (Full Stack)
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class OrderControllerIT {
    @Container static OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart");
    @Autowired TestRestTemplate restTemplate;

    @Test
    void should_createAndRetrieveOrder() {
        var request = new CreateOrderRequest(customerId, items);
        var created = restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var retrieved = restTemplate.getForEntity(
            "/api/v1/orders/" + created.getBody().id(), OrderResponse.class);
        assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

## Database Strategy

This project uses **Liquibase** for schema migrations. Changelogs live under
`src/main/resources/db/changelog/`, with `db.changelog-master.yaml` as the entry point.

- Never use `spring.jpa.hibernate.ddl-auto=update` in production
- Every schema change is version-controlled and reviewable
- Test migrations in CI against Oracle-compatible Testcontainers

## Observability

### Health and Metrics
- `/actuator/health` — liveness + readiness probes
- `/actuator/metrics` — Micrometer metrics (JVM, HTTP, DB pool, custom)
- `/actuator/prometheus` — Prometheus scrape endpoint (add `micrometer-registry-prometheus`)
- Configure via `management.endpoints.web.exposure.include=health,metrics,prometheus`

### Distributed Tracing (Micrometer Tracing + OpenTelemetry)
Uncomment the tracing dependencies in `pom.xml` / `build.gradle.kts`, then configure:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # 100% in dev, 0.1 in prod
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

Use `@Observed` on service methods:
```java
// service/OrderService.java
@Observed(name = "orders.create", contextualName = "create-order")
public OrderResponse createOrder(CreateOrderRequest request) { ... }
```

Trace context propagates automatically via W3C `traceparent` header.

### Structured Logging with Correlation
```java
// config/CorrelationIdFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        var requestId = Optional.ofNullable(req.getHeader("X-Request-Id"))
            .orElse(UUID.randomUUID().toString());
        MDC.put("requestId", requestId);
        res.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

## Security Architecture

- **Spring Security 7** (Spring Boot 4.x): Lambda DSL mandatory — `and()` chaining is removed
- **JWT / OAuth2 Resource Server**: stateless authentication via `http.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`
- **Method-level security**: `@PreAuthorize` on controllers (HTTP access control) or services (business rule authorization)
- **CSRF**: disabled for stateless REST APIs
- **CORS**: explicit `CorsConfigurationSource` bean — never `cors().disable()` or `"*"` origins in production
- **`config/SecurityConfig.java`**: all security rules live here

## Virtual Threads

Spring Boot 4.x enables virtual threads (Project Loom) by default. For Spring Boot 3.5.x, add to `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Implications for this architecture:**
- **JPA/JDBC**: blocking calls are fine — virtual threads park cheaply instead of blocking OS threads. No change required.
- **`@Transactional`**: works correctly. Transactions are still managed per-request unit of work.
- **`ThreadLocal`**: safe for short-lived, per-request state (e.g., MDC). Avoid long-lived `ThreadLocal` state — use `ScopedValue` (Java 21+) for request-scoped data that must not leak across requests.
- **Testcontainers**: fully compatible.
- **HikariCP**: compatible. Default pool size may need tuning for very high concurrency.

See `docs/adr/0009-virtual-threads.md` for the architectural decision rationale.

## Contract-First API with OpenAPI Generator

### Philosophy

The API contract is defined **before** implementation. The `openapi.yaml` spec is the
single source of truth for what the REST API looks like. Controller interfaces and model
classes are **generated** from it — developers never write these by hand.

```
src/main/resources/openapi/openapi.yaml
         │
         │  ./mvnw generate-sources
         ▼
target/generated-sources/openapi/
├── com/company/app/api/
│   └── OrdersApi.java              ← generated @RequestMapping interface
└── com/company/app/api/model/
    ├── CreateOrderRequest.java     ← generated request model (with @Valid)
    └── OrderResponse.java          ← generated response model
         │
         │  Controller implements generated interface
         ▼
controller/OrderController.java     ← your code: implements OrdersApi
mapper/OrderMapper.java             ← MapStruct: generated model ↔ JPA entity
```

### Key generation settings

| Setting | Value | Effect |
|---|---|---|
| `interfaceOnly` | `true` | Generates an interface, not a concrete class |
| `useSpringBoot3` | `true` | Uses `jakarta.*` namespace (required for SB3 and SB4) |
| `useJakartaEe` | `true` | Jakarta EE 10 annotations |
| `useBeanValidation` | `true` | `@NotNull`, `@Size`, etc. on model fields |
| `openApiNullable` | `false` | No `jackson-databind-nullable` dependency needed |
| `useTags` | `true` | One interface per tag (one tag = one controller) |
| `generateBuilders` | `true` | `builder()` factory on every model class |

### Organising the OpenAPI spec

- **One tag per resource** (e.g., `orders`, `customers`, `products`).
- Each tag produces one generated interface.
- Schema names become Java class names — use `PascalCase`.
- Use `$ref` to reuse schemas and avoid duplication.

```yaml
# src/main/resources/openapi/openapi.yaml (minimal example)
openapi: "3.1.0"
info:
  title: Order Management API
  version: "1.0.0"

paths:
  /api/v1/orders:
    post:
      tags: [orders]              # → generates OrdersApi interface
      operationId: createOrder
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        "201":
          description: Order created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'

components:
  schemas:
    CreateOrderRequest:
      type: object
      required: [customerId, items]
      properties:
        customerId:
          type: string
          format: uuid
        items:
          type: array
          items:
            $ref: '#/components/schemas/OrderItemRequest'
    OrderResponse:
      type: object
      properties:
        id:
          type: integer
          format: int64
        status:
          type: string
        total:
          type: number
          format: double
```

## MapStruct Object Mapping

MapStruct replaces all manual mapping code in the `mapper/` package. It generates
type-safe `*MapperImpl` classes at compile time from annotated interfaces.

### Typical mapper in this architecture

```java
// mapper/OrderMapper.java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    // Generated request model → JPA entity
    Order toEntity(CreateOrderRequest request);

    // JPA entity → generated response model
    OrderResponse toResponse(Order order);

    // Override a field name mapping
    @Mapping(source = "status.name", target = "statusLabel")
    OrderResponse toResponseWithLabel(Order order);
}
```

MapStruct generates `OrderMapperImpl implements OrderMapper` and registers it as a Spring
bean. The service injects the interface:

```java
// service/OrderService.java
private final OrderMapper orderMapper;  // Spring injects OrderMapperImpl

public OrderResponse createOrder(CreateOrderRequest request) {
    var entity = orderMapper.toEntity(request);   // no manual mapping needed
    var saved  = orderRepository.save(entity);
    return orderMapper.toResponse(saved);
}
```

### Request flow with OpenAPI + MapStruct

```
1. HTTP POST /api/v1/orders  (request body matches openapi.yaml schema)
2. → OrderController.createOrder(@Valid CreateOrderRequest request)
      — generated interface, generated request model, @Valid from generated annotations
3. → OrderService.createOrder(request)
4.   → OrderMapper.toEntity(request)  ← MapStruct conversion
5.   → OrderRepository.save(entity)
6. ← OrderMapper.toResponse(saved)   ← MapStruct conversion
7. ← 201 Created with OrderResponse body  (matches openapi.yaml response schema)
```

## Architecture Enforcement

Architecture rules are enforced through:
- **OpenAPI spec**: the `openapi.yaml` is the contractual boundary for the API layer
- **Code generation**: generated interfaces ensure controllers stay within the defined contract
- **MapStruct**: type-safe compile-time mappers catch DTO/entity mismatches early
- **Documentation**: `ARCHITECTURE.md` and `CONVENTIONS.md` define strict rules
- **Reference Implementation**: the CreateOrder vertical slice demonstrates correct patterns
- **Code Review**: human and AI reviewers validate layered compliance
- **ADR Trail**: decisions are recorded in `docs/adr/` for future reference
