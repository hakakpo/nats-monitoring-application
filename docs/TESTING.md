# Testing Guide

## Frameworks
- **JUnit 5** for all tests
- **Mockito** for mocking in unit tests
- **Testcontainers** for integration tests (DB, Redis, Kafka)
- **MockMvc** for controller tests (HTTP layer isolation)
- **REST Assured** or **TestRestTemplate** for full integration tests
- **AssertJ** for fluent assertions (preferred over Hamcrest)

## Test Classification

### Unit Tests (`src/test/java/.../service/`)
- Test service methods in isolation
- Mock all dependencies via `@Mock` / `@InjectMocks`
- Fast, no Spring context
- Naming: `should_{expectedBehavior}_when_{condition}()`

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private CustomerService customerService;
    @Mock private OrderMapper orderMapper;
    @InjectMocks private OrderService orderService;

    @Test
    void should_createOrder_when_validRequest() {
        // given
        var request = new CreateOrderRequest(customerId, items);
        when(orderMapper.toEntity(request)).thenReturn(testOrder);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        // when
        var result = orderService.createOrder(request);

        // then
        assertThat(result).isEqualTo(expectedResponse);
        verify(customerService).validateCreditLimit(customerId);
        verify(orderRepository).save(any());
    }
}
```

### Controller Tests (`src/test/java/.../controller/`)
- Use `@WebMvcTest` to load only the web layer
- Mock services with `@MockBean`
- Test HTTP status codes, request validation, response format

```java
@Tag("unit")
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private OrderService orderService;

    @Test
    void should_return201_when_orderCreated() throws Exception {
        when(orderService.createOrder(any())).thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(APPLICATION_JSON)
                .content(validRequestJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void should_return400_when_requestInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
```

### Repository Tests (`src/test/java/.../repository/`)
- Use `@DataJpaTest` for lightweight JPA testing
- Use Testcontainers for a real database
- Test custom queries and derived query methods

```java
@Tag("integration")
@DataJpaTest
@Testcontainers
class OrderRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private OrderRepository orderRepository;

    @Test
    void should_findOrdersByStatus() {
        orderRepository.save(testOrder(OrderStatus.CREATED));
        orderRepository.save(testOrder(OrderStatus.COMPLETED));

        var results = orderRepository.findByStatus(OrderStatus.CREATED);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(OrderStatus.CREATED);
    }
}
```

### Integration Tests (`src/test/java/.../integration/`)
- Use `@SpringBootTest` with `@Testcontainers`
- Test full request flow including database
- Naming: `{ClassName}IT`

```java
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class OrderControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired private TestRestTemplate restTemplate;

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

## Coverage Requirements
- Service layer: 80%+ line coverage
- Controllers: tested via @WebMvcTest (happy path + validation errors)
- Repositories: tested via @DataJpaTest for custom queries
- Utilities: 90%+ coverage

## What NOT to Test
- Getters/setters on DTOs (records handle this)
- Framework code (Spring Security filters, Jackson serialization)
- Third-party library internals
- Simple Spring Data derived query methods (e.g., `findById`)

## Running Tests

### Maven
```bash
./mvnw test                       # All tests
./mvnw test -Dgroups=unit         # Unit tests only
./mvnw test -Dgroups=integration  # Integration tests only
./mvnw test jacoco:report         # With coverage report
```

### Gradle
```bash
./gradlew test                    # All tests
./gradlew test -Dgroups=unit      # Unit tests only
./gradlew test -Dgroups=integration # Integration tests only
./gradlew jacocoTestReport        # With coverage report
```

## WireMock — Stubbing External HTTP Services

Use WireMock to test service classes that call external HTTP APIs (payment gateways, notification services, etc.). Add `@WireMockTest` to the test class — no manual server setup needed.

```java
@Tag("integration")
@WireMockTest
class PaymentServiceTest {

    @Autowired
    PaymentService paymentService;

    @Test
    void should_processPayment_when_gatewayRespondsOk(WireMockRuntimeInfo wm) {
        // Given: stub payment gateway
        stubFor(post(urlEqualTo("/v1/charges"))
            .willReturn(okJson("""
                {"id": "ch_123", "status": "succeeded", "amount": 5000}
                """)));

        // When
        var result = paymentService.charge(orderId, BigDecimal.valueOf(50));

        // Then
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(postRequestedFor(urlEqualTo("/v1/charges")));
    }
}
```

**Rules:**
- WireMock tests live in `service/` alongside the service under test
- Tag as `@Tag("integration")` — WireMock tests require a running stub server
- Use `@WireMockTest(httpPort = 8089)` to fix the port if the service needs a configured URL

## ArchUnit — Architecture Rule Tests (Optional)

ArchUnit automatically enforces architectural boundaries as failing tests. Uncomment `archunit-junit5` in `pom.xml` / `build.gradle.kts` to enable.

```java
// src/test/java/com/company/app/arch/ArchitectureTest.java
@Tag("unit")
@AnalyzeClasses(packages = "com.company.app")
class ArchitectureTest {

    // Rule 1: controllers never call repositories
    @ArchTest
    static final ArchRule controllersMustNotCallRepositories =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..repository..");

    // Rule 2: services never access controllers
    @ArchTest
    static final ArchRule servicesMustNotDependOnControllers =
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .resideInAPackage("..controller..");

    // Rule 3: repositories never import service classes
    @ArchTest
    static final ArchRule repositoriesMustNotAccessServices =
        noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat()
            .resideInAPackage("..service..");
}
```

See `docs/adr/0005-no-archunit.md` for the decision to make this optional.

## JaCoCo Coverage Exclusions

Generated code and MapStruct implementations are excluded from coverage by default (configured in `pom.xml` / `build.gradle.kts`):
- `**/api/**` — generated controller interfaces and DTOs
- `**/*MapperImpl*` — generated MapStruct implementations

Coverage targets apply to hand-written code only:
- Service layer: 80%+ line coverage
- Utilities and helpers: 90%+ line coverage
