# ADR-0004: Database Migration Tool — Flyway vs Liquibase

## Status

accepted (both supported — each project picks ONE)

## Context

Database schema changes must be tracked, versioned, and reproducible. We need a migration tool
that integrates well with Spring Boot and supports team collaboration.

Both Flyway and Liquibase are mature, well-supported tools. Rather than mandating one for all
projects, we support both in the template and let each project team choose based on their needs.

## Decision

Support **both Flyway and Liquibase** in the project template. Each project picks ONE tool and
removes the other from the build file.

### How to Detect Which is Active
- **Flyway**: `src/main/resources/db/migration/` contains `V*__.sql` files
- **Liquibase**: `src/main/resources/db/changelog/` contains `db.changelog-master.yaml`
- Also check the build file — only one migration dependency should be uncommented

### When to Choose Flyway
- Team prefers raw SQL for migrations
- Straightforward schema evolution
- Simplicity is a priority

### When to Choose Liquibase
- Team prefers declarative changelogs (YAML/XML)
- Rollback support is important
- Enterprise features needed (preconditions, contexts, labels)

### Shared Rules (Both Tools)
- **Never use `spring.jpa.hibernate.ddl-auto=update` in production**
- All schema changes must go through versioned migrations
- Test migrations in CI against a real database (Testcontainers)
- One migration per schema change (don't combine unrelated changes)

## Consequences

### Positive
- Teams can pick the tool that matches their workflow
- Both tools are first-class citizens in the Spring Boot ecosystem
- Template works for a wider range of teams

### Negative (Trade-offs)
- Minor template clutter (both configurations present initially)
- AI agents must detect which tool is active before generating migration files

## Alternatives Considered

### Alternative 1: Hibernate auto-DDL (`ddl-auto=update`)
- Zero migration file management
- **Rejected because**: Not reproducible, not auditable, dangerous in production. Schema changes
  cannot be reviewed in PRs

### Alternative 2: Manual SQL Scripts
- Full control over every statement
- **Rejected because**: No version tracking, no rollback support, easy to miss changes

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Decision accepted |
