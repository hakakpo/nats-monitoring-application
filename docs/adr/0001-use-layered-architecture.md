# ADR-0001: Use Classic Layered Architecture

## Status

accepted

## Context

We needed to choose an architectural pattern for a Spring Boot application. The team considered
several options. Key factors in the decision:

- **Team familiarity**: The majority of the team has experience with classic Spring Boot layered
  applications and is productive in this style
- **Domain complexity**: The business domain is moderately complex but does not require the
  strict isolation that hexagonal architecture provides
- **Time to market**: We need to deliver features quickly and cannot afford the learning curve
  of a more complex architecture
- **Framework alignment**: Spring Boot's conventions (annotations, auto-wiring, starter ecosystem)
  naturally support a layered approach
- **Onboarding**: New team members (and AI agents) should be productive quickly

## Decision

We decided to adopt a **Classic Layered Architecture** with three main layers:

1. **Controller layer** (`controller/`): Handles HTTP concerns — request validation, response
   mapping, status codes. Annotated with `@RestController`.
2. **Service layer** (`service/`): Contains all business logic and orchestration. Annotated with
   `@Service` and `@Transactional`.
3. **Repository layer** (`repository/`): Handles data access via Spring Data JPA. Extends
   `JpaRepository` interfaces.

Supporting packages: `model/` (JPA entities), `dto/` (request/response records), `mapper/`
(conversion between DTOs and entities), `exception/` (custom exceptions and global handler),
`config/` (Spring configuration).

The **#1 rule**: business logic lives ONLY in the service layer. Controllers are thin, and
repositories contain no business logic.

## Consequences

### Positive
- **Low learning curve**: Standard Spring Boot pattern that most Java developers know
- **Fast development**: Spring's conventions reduce boilerplate and accelerate feature delivery
- **Rich ecosystem**: Full access to Spring Boot starters, auto-configuration, and community support
- **AI-friendly**: All major AI coding tools (Copilot, Cursor, Claude) generate idiomatic
  layered Spring Boot code by default
- **Clear responsibility**: Each layer has a well-understood role
- **Easy testing**: Spring provides `@WebMvcTest`, `@DataJpaTest`, and `@SpringBootTest` for
  each layer individually

### Negative (Trade-offs)
- **Framework coupling**: Business logic in the service layer may accumulate Spring-specific
  concerns (e.g., `@Transactional`, `@Cacheable`). If we ever need to switch frameworks, the
  service layer will require significant refactoring
- **Less domain purity**: Unlike hexagonal architecture, the domain entities are JPA entities.
  This means persistence concerns leak into the model layer
- **Risk of fat services**: Without strict port/adapter boundaries, services can grow large.
  We mitigate this with code review and splitting services by aggregate
- **DTO discipline required**: Without architectural enforcement, developers might be tempted
  to return JPA entities directly. We enforce DTO separation through convention and review

## Alternatives Considered

### Alternative 1: Hexagonal Architecture (Ports & Adapters)
- Domain layer is pure Java with zero framework dependencies
- Strict port/adapter boundaries enforce clean separation
- **Rejected because**: Higher complexity, more boilerplate (separate domain and JPA entities,
  BeanConfig wiring), steeper learning curve, and the domain complexity does not justify the
  additional overhead at this stage

### Alternative 2: Clean Architecture (Uncle Bob)
- Similar to hexagonal but with more explicit layers (entities, use cases, interface adapters)
- **Rejected because**: Even more layers and abstraction than hexagonal. Would over-engineer
  our current needs

### Alternative 3: CQRS + Event Sourcing
- Separate read and write models with event-driven state
- **Rejected because**: Significant complexity increase. May be revisited if the domain grows
  in complexity or event replay becomes a requirement

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Decision accepted |
