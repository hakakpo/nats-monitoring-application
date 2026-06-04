# ADR-0002: PostgreSQL 16 as Primary Database

## Status

accepted

## Context

We needed to select a relational database for the application. Key considerations:

- **JSON support**: Some entities require semi-structured data storage (e.g., order metadata,
  customer preferences). We need first-class JSON querying, not just blob storage
- **ACID compliance**: Financial data (orders, invoices) requires strict transactional guarantees
- **Advanced indexing**: GIN indexes for full-text search and JSONB, partial indexes for
  performance optimization
- **Ecosystem maturity**: The database must have strong Spring Data JPA support, Testcontainers
  integration, and a large community
- **Team experience**: Several team members have production experience with PostgreSQL

## Decision

We chose **PostgreSQL 16** as the primary database.

- Driver: `org.postgresql:postgresql` (runtime scope)
- ORM: Spring Data JPA (Hibernate 6.x under the hood)
- Testing: Testcontainers with `PostgreSQLContainer`
- Migrations: Flyway or Liquibase (see ADR-0004)

## Consequences

### Positive
- Native JSON/JSONB support with indexing and querying
- Advanced features: CTEs, window functions, materialized views, partial indexes
- Excellent Spring Boot and Testcontainers support
- Strong ACID compliance and data integrity guarantees
- Active community and extensive documentation

### Negative (Trade-offs)
- Slightly more complex setup than H2 for local development (mitigated by Testcontainers
  and Docker Compose)
- Some hosting environments default to MySQL; requires explicit provisioning
- Team members less familiar with PostgreSQL-specific features will need onboarding

## Alternatives Considered

### Alternative 1: MySQL 8.0
- Widely available and familiar
- JSON support exists but is less mature than PostgreSQL's JSONB
- **Rejected because**: Weaker JSON querying, no partial indexes, less advanced window functions

### Alternative 2: H2 (in-memory for all environments)
- Zero-configuration local development
- **Rejected because**: Not suitable for production. Behavior differences from production DB
  lead to bugs. Testcontainers provides a better solution for local development

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Decision accepted |
