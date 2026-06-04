# ADR 0009 — Virtual Threads (Project Loom)

**Date:** 2026-04-08
**Status:** Accepted

## Context

Java 21 introduced **virtual threads** (Project Loom, JEP 444) as a stable feature.
Virtual threads are lightweight, JVM-managed threads that park instead of blocking OS threads,
enabling high concurrency for I/O-bound workloads without reactive programming.

Spring Boot 3.2+ supports virtual threads via `spring.threads.virtual.enabled=true`.
Spring Boot 4.x enables virtual threads by default.

This project targets Java 21 and Spring Boot 4.x.

## Decision

**Enable virtual threads.** Accept Spring Boot 4.x's default behavior. For Spring Boot 3.5.x,
explicitly set `spring.threads.virtual.enabled=true` in `application.yml`.

## Rationale

1. **No code changes required** — synchronous, readable Spring MVC code gains concurrency benefits automatically.
2. **JDBC is compatible** — blocking a virtual thread is cheap; no need for R2DBC or reactive patterns.
3. **JPA/`@Transactional` still works** — transactions are managed per unit-of-work, not per thread lifecycle.
4. **Testcontainers compatible** — no changes to test setup.
5. **Simpler model** — avoids the complexity of `Mono`/`Flux` for services that don't need reactive pipelines.

## Consequences

### Benefits
- Higher throughput for I/O-bound REST endpoints with no code changes
- `@Async` methods automatically use virtual threads when enabled
- Simpler debugging — virtual thread stack traces are readable (unlike reactive chains)

### Constraints and Rules
- **`ThreadLocal` risk:** Avoid storing long-lived state in `ThreadLocal` — with millions of virtual threads, this can cause memory pressure. Prefer `ScopedValue` (Java 21+, JEP 446) for request-scoped data.
- **Synchronized blocks:** Avoid long `synchronized` blocks — they pin virtual threads to OS threads (called "pinning"), reducing concurrency benefits. Prefer `ReentrantLock`.
- **Connection pool:** HikariCP is compatible. The default pool size (10) may be a bottleneck at very high concurrency — monitor and tune if needed.
- **Services use virtual threads for all I/O** — service methods calling repositories, external APIs, or messaging brokers automatically benefit from virtual thread scheduling.

## Implementation

```yaml
# application.yml — Spring Boot 3.5.x (explicit)
spring:
  threads:
    virtual:
      enabled: true

# Spring Boot 4.x — enabled by default, no config needed
```

## What NOT To Do
- Do NOT use `ThreadLocal` for long-lived, per-request state — use Spring's `RequestContextHolder` or `ScopedValue`
- Do NOT use `synchronized` blocks with expensive I/O inside — use `ReentrantLock` instead
- Do NOT disable virtual threads to work around a problem — diagnose the root cause
- Do NOT use reactive WebFlux just for concurrency — virtual threads make it unnecessary for most REST APIs
