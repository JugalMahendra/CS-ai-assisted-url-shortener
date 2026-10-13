# Requirements Coverage Matrix

Maps the assignment brief to concrete evidence in this repository, with an honest status.
Status: ✅ PASS · 🟡 PARTIAL. Test-verification status is stated plainly at the bottom.

## Core requirements (brief §4)
| # | Requirement | Evidence | Status |
|---|---|---|---|
| 1 | Requirement understanding (intent, ambiguity) | `documents/scenarios-and-validation.md` → Scenario 3 (ambiguous: 302/301, expiry, SSRF depth, alias scope); decisions in the AI log | ✅ |
| 2 | Task decomposition (actionable tasks, sequencing) | task-by-task entries in `docs/ai-engineering-log.md`; "Decomposition" sections per scenario | ✅ |
| 3 | Codebase reasoning (brownfield) | `scenarios-and-validation.md` → Scenario 2 (real Flyway-autoconfig & Spring Boot 4 fixes, with investigation) | ✅ |
| 4 | AI-assisted execution (prompts, accepted/edited/rejected, quality gates, oversight) | `docs/ai-engineering-log.md` (prompts, decisions, corrections, verbatim validation output) | ✅ |
| 5 | Engineering output (code, schema, tests, docs) | `src/main/**` modules; Flyway `V1–V3`; 86 tests; this docset | ✅ |
| 6 | Validation & risk control (risks, guardrails) | `documents/engineering-summary.md` risk table + documented residual SSRF risks; validation tests | ✅ |
| 7 | Controlled oversight (engineer approves) | per-task approval gates and sign-offs in the AI log | ✅ |
| 8 | Final engineering summary | `documents/engineering-summary.md` | ✅ |

## Deliverables (brief §5)
| Deliverable | Evidence | Status |
|---|---|---|
| Working prototype (runnable end-to-end) | `README.md` Quick start · `docker-compose.yml` · `mvn spring-boot:run` | ✅ |
| Architecture overview (components, flow, decisions) | `documents/architecture.md` (+ ER & sequence diagrams) | ✅ |
| Three scenarios (greenfield/brownfield/ambiguous) | `documents/scenarios-and-validation.md` | ✅ |
| Setup instructions | `README.md` (Quick start · Configuration · Testing) | ✅ |
| Testing approach, limitations, trade-offs | `documents/engineering-summary.md` · README → Testing | ✅ |

## Functional / API coverage
| Capability | Code | Tests |
|---|---|---|
| Create (Base62-from-sequence or custom alias) | `shortening/**` | `Base62Test`, `CustomAliasValidatorTest`, `ShortCodeServiceTest`, `LinkControllerTest`, `CreateLinkIntegrationTest` |
| Redirect 302 · 404 · 410 | `redirect/**` | `RedirectServiceTest`, `RedirectIntegrationTest` |
| Click recording + consistent counter (one tx) | `RedirectService`, `ShortLinkRepository.incrementClickCount` | `RedirectIntegrationTest` |
| Analytics (aggregate + per-day series) | `analytics/**` | `AnalyticsIntegrationTest` |
| URL validation + unsafe-target checks | `validation/UrlValidator` | `UrlValidatorTest` |
| RFC 7807 error model | `platform/web/GlobalExceptionHandler` | `LinkControllerTest`, integration tests |
| Health | Spring Boot Actuator | `UrlShortenerApplicationTests` |
| OpenAPI / Swagger UI | `src/main/resources/static/docs/` | served statically |

## Testing verification (honest)
- **Unit (no Docker): 63 tests — verified green** locally.
- **Integration (Testcontainers):** schema/migrations, code generation, and create/redirect/analytics
  are written and compile; they execute under `mvn verify` on a Docker host. The local dev sandbox
  blocks the Docker socket, so they were not run there — **GitHub Actions CI
  (`.github/workflows/ci.yml`) runs the full 86-test suite** (CI runners provide Docker), which is the
  authoritative green signal.
- No test result is claimed as passing unless it actually ran and passed.

## Not implemented (documented, out of prototype scope)
Authentication, rate limiting, expiry sweep, multi-node, async/queue — see
`documents/engineering-summary.md` → *Limitations & future scope*.
