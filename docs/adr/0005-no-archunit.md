# ADR-0005: No ArchUnit — Rely on Documentation and Code Review

## Status

Reconsidered — Now Optional

## Context

We need to enforce architectural rules in our layered architecture:
- Controllers must not inject repositories directly
- Services must not return JPA entities to controllers
- Business logic must live in services, not controllers or repositories
- DTOs must be used at the API boundary

We evaluated automated tools like ArchUnit to enforce these rules programmatically.

## Decision

We decided to **NOT use ArchUnit** for architecture enforcement. Instead, we rely on:

1. **Documentation as the contract**: `AGENTS.md`, `ARCHITECTURE.md`, and `CONVENTIONS.md`
   define all rules clearly
2. **Reference implementation**: The CreateOrder vertical slice demonstrates the correct pattern
3. **Code review**: Human reviewers and AI agents check for violations
4. **AI agent instructions**: All agent config files (`.cursorrules`, `CLAUDE.md`, etc.) embed
   the rules so AI-generated code follows the architecture by default
5. **Regular architecture review**: Periodic review of the codebase for drift

### Architecture Compliance Checklist (for Code Reviews)
- [ ] Controllers only inject services (never repositories)
- [ ] Controllers accept/return DTOs (never JPA entities)
- [ ] Business logic is in the service layer
- [ ] Services use `@Transactional` for write operations
- [ ] Constructor injection (no `@Autowired` on fields)
- [ ] New endpoints have corresponding tests
- [ ] Database changes have migration files

## Consequences

### Positive
- **Simplicity**: No additional test dependency or configuration
- **Readable rules**: Documentation is more expressive than ArchUnit DSL
- **AI-compatible**: AI agents can read and follow markdown rules directly
- **Focus on understanding**: Developers learn the architecture, not just pass tests
- **Faster test suite**: No additional architecture tests slowing down the build

### Negative (Trade-offs)
- **Human discipline required**: Without automated enforcement, violations can slip through
- **Gradual drift**: Over time, small violations can accumulate without automated detection
- **Onboarding**: New developers must read the docs (not enforced by a failing test)
- **Code review burden**: Reviewers must actively check for architecture violations

### When to Reconsider
- If violations become frequent despite code review
- If the team grows beyond 8-10 developers
- If onboarding new members becomes difficult
- If AI agents consistently generate non-compliant code

If reconsidered, add ArchUnit with rules like:
```java
@ArchTest
static final ArchRule controllers_should_not_depend_on_repositories =
    noClasses().that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..repository..");
```

## Alternatives Considered

### Alternative 1: ArchUnit Tests
- Automated architecture checks as unit tests
- **Rejected because**: Adds complexity and a learning curve for rules that are better expressed
  in documentation. Can be added later if needed

### Alternative 2: Maven Enforcer Plugin
- Can restrict dependencies at the module level
- **Rejected because**: Works at the module level, not package level. Our layered architecture
  uses packages within a single module

### Alternative 3: Checkstyle / PMD Custom Rules
- Can detect some import violations
- **Rejected because**: Significant effort to write custom rules for architectural patterns.
  Better suited for formatting and code style

---

## Changelog

| Date | Event |
|------|-------|
| 2025-01-15 | Decision accepted |
| 2026-04-08 | Status updated: ArchUnit now optional — see Amendment below |

## Amendment — 2026-04-08

**Status updated to: Reconsidered — ArchUnit is now Optional**

### What Changed
ArchUnit has matured significantly since this ADR was first written. It is now widely used in production Spring Boot projects and catches real architectural violations automatically.

### Updated Recommendation
ArchUnit is **optional but recommended** for:
- Teams of 3+ developers
- Projects with a lifespan > 6 months
- Projects where multiple developers contribute to the same codebase

### How to Enable
1. Uncomment `archunit-junit5` in `pom.xml` or `build.gradle.kts`
2. Create `src/test/java/com/company/app/arch/ArchitectureTest.java`
3. See `docs/TESTING.md` for example arch rules

### Example Rules (Layered)
```java
@AnalyzeClasses(packages = "com.company.app")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersMustNotCallRepositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule servicesMustNotDependOnControllers =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");
}
```

### Original Decision (preserved)
The original rationale for not mandating ArchUnit remains valid for small teams and short-lived projects. This amendment makes it opt-in rather than prohibited.
