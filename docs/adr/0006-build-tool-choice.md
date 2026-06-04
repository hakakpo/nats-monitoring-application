# ADR-0006: Build Tool Choice — Maven vs Gradle

## Status

Deferred to project teams (both supported)

## Context

Both Maven and Gradle are valid choices for Spring Boot 3.x / Java 21 projects. Rather than
mandating one, we provide both configurations and let each project team decide.

| Aspect | Maven | Gradle (Kotlin DSL) |
|--------|-------|---------------------|
| Build speed | Moderate | Fast (daemon, caching) |
| Configuration | XML (pom.xml) | Kotlin DSL (build.gradle.kts) |
| Learning curve | Low (convention-based) | Moderate (more flexible) |
| Enterprise adoption | Very high | Growing |
| IDE support | Excellent | Excellent |
| Spring Boot support | First-class | First-class |
| Dependency management | Mature | Mature |
| Plugin ecosystem | Large | Large |
| Reproducibility | High (wrapper) | High (wrapper) |

## Decision

Both **Maven** and **Gradle** configurations are provided in the template. Each project team
picks ONE and deletes the other's files:

- **Choose Maven** if: Team prefers convention over configuration, enterprise environment,
  familiarity is high
- **Choose Gradle** if: Team prefers faster builds, Kotlin DSL expressiveness, more flexibility

### After Choosing
- **Maven**: Delete `build.gradle.kts`, `settings.gradle.kts`, `gradle/` directory
- **Gradle**: Delete `pom.xml`, `.mvn/` directory

## Consequences

### Positive
- Teams use the tool they are most productive with
- No forced migration from a familiar build tool
- Template works for a wider range of teams

### Negative (Trade-offs)
- AI agents must detect which build tool is active before generating commands
- Documentation shows both Maven and Gradle commands (minor clutter)

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Deferred to project teams |
