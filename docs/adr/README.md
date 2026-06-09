# Architecture Decision Records (ADRs)

## What are ADRs?

Architecture Decision Records are lightweight documents that capture significant architectural decisions made on this project. Each ADR records:
- The issue or problem that prompted the decision
- The choice that was made
- The consequences (positive and negative) of that choice
- Alternative options that were considered

ADRs serve as a historical record of **why** decisions were made, not just **what** was decided. This helps future team members (including AI agents) understand the context and rationale behind the codebase.

## When to Create an ADR

Create an ADR when you make a choice that affects:
- Project structure and organization
- External dependencies (frameworks, databases, libraries)
- Architectural patterns and design principles
- Integration strategies
- Technology stack decisions
- Testing strategies

**Do not** create ADRs for routine coding tasks, bug fixes, or feature implementations that follow existing patterns.

## How to Use ADRs

### Creating a New ADR

1. **Copy the template** from `0000-adr-template.md`
2. **Number sequentially**: Use the next available four-digit number (e.g., `0001`, `0002`)
3. **Name descriptively**: Use a short kebab-case title that summarizes the decision
4. **File naming**: `NNNN-short-title.md` (e.g., `0001-use-layered-architecture.md`)
5. **Fill in all sections**: Context, Decision, Consequences, Alternatives Considered
6. **Set the Status** to `proposed` initially; update to `accepted` after approval
7. **Commit to version control** with a clear commit message

### File Naming Convention

```
docs/adr/NNNN-short-title.md
     ^^^^
     Sequential four-digit number
```

Examples:
- `0001-use-layered-architecture.md`
- `0002-postgresql-over-mysql.md`
- `0003-dto-entity-separation.md`

### Updating Status

- **proposed**: Decision is under discussion
- **accepted**: Decision has been approved and is in use
- **deprecated**: Decision is no longer relevant (but kept for history)
- **superseded**: Decision was replaced by a newer ADR (link to the new one)

## Reading ADRs

Start with the most recent ADRs to understand the current architecture. Older ADRs provide historical context for why things are the way they are.

All ADRs should be reviewed before making changes that might conflict with previous decisions.

## ADRs in This Project

- **0001**: Use Classic Layered Architecture
- **0002**: PostgreSQL 16
- **0003**: DTOs Separate from JPA Entities
- **0004**: Database Migration Tool — Flyway vs Liquibase (both supported, pick one per project)
- **0005**: No ArchUnit (Rely on Documentation and Code Review)
- **0006**: Build Tool Choice — Maven vs Gradle (both supported, pick one per project)
- **0007**: Contract-First API Development with OpenAPI Generator
- **0008**: MapStruct for Object Mapping
- **0009**: Virtual Threads
- **0010**: Liquibase for Oracle Migrations

See each ADR for details.
