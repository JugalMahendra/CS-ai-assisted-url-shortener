# Final Engineering Summary

## Plan & rationale
A modular-monolith URL shortener (Java 21 / Spring Boot 4.1, PostgreSQL + Flyway, Testcontainers), built
task-by-task with engineer approval gates: scaffold → schema/entities → URL validation → short-code
generation + aliases → create API → redirect + click recording → analytics. AI (Claude Code) was used as
an accelerator; every change was reviewed and owned by the engineer, with per-change traceability in
[`../docs/ai-engineering-log.md`](../docs/ai-engineering-log.md).

## Artifacts
- **Code:** `shortening`, `redirect`, `analytics`, `validation`, `platform` modules.
- **Schema (Flyway):** `V1` baseline, `V2` core schema (`short_link`, `click_event`, indexes), `V3`
  `short_code_seq`.
- **APIs:** `POST /api/v1/links`, `GET /{code}`, `GET /api/v1/links/{code}/analytics`, `GET /actuator/health`.
- **Tests:** 86 total — 63 unit + 23 Testcontainers/integration.
- **Docs:** `README.md`, `documents/architecture.md`, `documents/scenarios-and-validation.md`, this
  summary, and the AI engineering log.

## Locked decisions
302 (configurable); Base62-of-sequence + UNIQUE constraint; transactional click recording (no async);
410 for expired/deactivated and 404 for unknown; http/https only with no destination fetch; portable
in-repo Maven settings (no office/corporate mirror).

## Risks, trade-offs & validation
| Risk | Mitigation | Residual |
|---|---|---|
| URL validation / unsafe redirect | scheme allowlist + literal private-host block; no fetch/DNS | DNS rebinding, alt IP encodings, inherent open-redirect (documented) |
| Short-code collisions | DB sequence (collision-free) + UNIQUE + retry | none material |
| Analytics consistency | counter increment + event insert in one transaction | small synchronous redirect latency (accepted) |
| Expiration | lazy `410` on read | `EXPIRED` not persisted until a sweep (deferred) |
| Abuse / enumeration | input caps; reserved words | no rate limiting (future); sequential codes are guessable |
| Database failure | transactional writes; actuator DB health | no multi-node/failover (prototype scope) |

## Assumptions
No authentication (prototype); single node; client IP via `getRemoteAddr()` (no `X-Forwarded-For`);
UTC day buckets for the time series; PostgreSQL only.

## Testing approach & honest status
- **Unit (no Docker):** Base62 codec, alias rules, URL validation, controller mapping, and redirect
  expiry/hash helpers — **63 tests, verified green** in the dev environment
  (`mvn -o -Dtest=Base62Test,CustomAliasValidatorTest,UrlValidatorTest,LinkControllerTest,RedirectServiceTest test`).
- **Integration (Testcontainers + real PostgreSQL):** schema/migrations, sequence-based code generation,
  and create/redirect/analytics over real HTTP.
- **Observed green so far:** the T1–T4 full suite ran green via `mvn verify` (**71 tests**), plus the 63
  unit tests above. The **create/redirect/analytics integration tests (12)** are written and compile but
  have **not yet been executed**; running `mvn verify` on a Docker host is the remaining step to confirm
  all **~86**. No test in this repository is claimed as passing unless it actually ran and passed.

## Limitations & future scope
No auth/authz; no rate limiting; single-node; synchronous click recording; lazy expiry (no background
sweep); PostgreSQL-specific analytics query; API documented in the README (no OpenAPI/Swagger UI);
app runs via `mvn`/JAR (not containerized). Future: authentication, rate limiting, an expiry sweep,
OpenAPI (springdoc), app containerization + app-in-compose, static analysis (Checkstyle/SpotBugs), and
load testing.

## Engineer sign-off
The core create → redirect → analytics flow is implemented and unit-verified (63/63 in this
environment); full integration verification is pending the engineer's `mvn verify` on a Docker host
(command in the README / testing section). AI assistance was used throughout with human review of every
change; notable issues were caught and fixed during review (the Spring Boot 4 Flyway auto-config module,
the Testcontainers BOM, the relocated MVC test slice, the silently-skipped `*IT` test), and an earlier
inaccurate "Flyway applied" note was corrected in the log.

— Engineer: Jugal Mahendra · 2026-10-12 · *(full-suite `mvn verify` sign-off pending the Docker run)*
