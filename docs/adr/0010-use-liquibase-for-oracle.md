# ADR-0010: Use Liquibase for Oracle Migrations

## Status

accepted

## Context

The application is deployed with Oracle Database 19.3c. The existing Flyway SQL migrations used
database-specific constructs such as `IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, and `BOOLEAN`
columns that are not portable to Oracle.

Schema management still needs to remain versioned, reviewable, and compatible with
`spring.jpa.hibernate.ddl-auto=validate`.

## Decision

Use Liquibase as the only active database migration tool.

- Runtime migration dependency: `org.liquibase:liquibase-core`
- Oracle runtime driver: `com.oracle.database.jdbc:ojdbc11`
- Oracle Spring profile: `application-oracle.yml`
- Changelog entry point: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Flyway scripts under `src/main/resources/db/migration/` are removed from the active codebase

Liquibase changelogs use abstract data types such as `BOOLEAN`, `BIGINT`, `TIMESTAMP`, and `CLOB`
so Liquibase can generate the correct database-specific SQL for Oracle and H2.

## Consequences

### Positive

- Oracle 19.3c receives generated DDL instead of non-portable H2/PostgreSQL-style SQL.
- Existing local H2 development remains supported through the same changelog.
- Preconditions allow the migration to be marked as already applied when tables or columns already exist.
- Hibernate validation remains enabled; Hibernate does not manage schema mutations.

### Negative (Trade-offs)

- Changelogs are more verbose than raw SQL migrations.
- Developers must use Liquibase syntax for future schema changes.
- Existing databases that were previously managed by Flyway need an operational migration plan for the old `flyway_schema_history` metadata if that table exists.

## Alternatives Considered

### Alternative 1: Keep Flyway and Rewrite SQL per Database

Rejected because the current requirement is to support Oracle 19.3c with Liquibase, and maintaining
database-specific SQL variants would increase operational complexity.

### Alternative 2: Hibernate `ddl-auto=update`

Rejected because schema changes would no longer be versioned, reviewable, or safe for production.

## Changelog

| Date | Event |
|------|-------|
| 2026-06-09 | Decision accepted |
