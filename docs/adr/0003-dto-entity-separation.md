# ADR-0003: Separate DTOs from JPA Entities

## Status

accepted

## Context

In a layered architecture, a common question is whether to expose JPA entities directly in
REST API responses or to use separate DTOs (Data Transfer Objects).

Exposing JPA entities directly is tempting because it reduces code, but it creates several
problems:

- **Tight coupling**: API consumers depend directly on the database schema
- **Security risk**: All entity fields (including internal ones) are serialized unless explicitly
  excluded with `@JsonIgnore`
- **Lazy loading issues**: Returning entities outside a transaction causes
  `LazyInitializationException` or triggers N+1 queries with eager loading
- **Circular references**: Bidirectional JPA relationships cause infinite recursion in JSON
  serialization
- **Breaking changes**: Any database schema change becomes an API breaking change
- **Over-fetching**: Clients receive more data than they need

## Decision

We use **separate DTO classes** (Java records) for all API request and response types.
JPA entities are NEVER exposed directly in REST API responses.

### Structure
- **Request DTOs**: `dto/CreateOrderRequest.java` — records with Jakarta Validation annotations
- **Response DTOs**: `dto/OrderResponse.java` — plain records, shaped for the API consumer
- **Mappers**: `mapper/OrderMapper.java` — convert between DTOs and entities

### Rules
1. Controllers accept and return DTOs only
2. Services accept DTOs (or domain commands) and return DTOs
3. JPA entities stay within the service and repository layers
4. Mappers live in the `mapper/` package (MapStruct recommended, manual mapping acceptable)

## Consequences

### Positive
- **API stability**: Database schema changes don't break the API
- **Security**: Only intended fields are exposed to clients
- **No lazy-loading issues**: DTOs are simple records with no JPA proxies
- **Flexibility**: Different DTOs for different endpoints (create vs. update vs. list)
- **Clean serialization**: No circular reference issues, no `@JsonIgnore` annotations needed
- **Testability**: DTOs are easy to construct in tests

### Negative (Trade-offs)
- **More code**: Each entity typically has 2-4 DTO classes + a mapper
- **Mapper maintenance**: When entities change, mappers and DTOs must be updated too
- **Learning curve**: New developers may initially find the indirection confusing
- **MapStruct setup**: If using MapStruct, annotation processing configuration is needed

## Alternatives Considered

### Alternative 1: Expose JPA Entities Directly
- Less code initially
- **Rejected because**: Causes all the problems described in the Context section. Technical debt
  compounds quickly as the API grows

### Alternative 2: Use `@JsonView` for Field Filtering
- Single entity class with Jackson views to control serialization
- **Rejected because**: Still couples the API to the entity structure. Views become hard to
  manage as the number of endpoints grows. Doesn't solve lazy-loading or circular reference issues

### Alternative 3: GraphQL (Let Clients Choose Fields)
- Clients select exactly the fields they want
- **Rejected because**: Adds significant infrastructure complexity. Not justified for a REST API
  with well-defined endpoints

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Decision accepted |
