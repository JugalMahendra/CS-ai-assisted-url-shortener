# AI-Assisted Engineering Log

This log records how AI assistance was used during development, for traceability and engineer
ownership. Each significant change gets one entry. **Validation evidence is filled only with actual
observed output from a real run — never pre-filled or assumed.**

## Entry template

```
### [YYYY-MM-DD HH:MM] <Change title> (Task T#)
- Scenario: greenfield | brownfield | ambiguous
- Intent / problem:
- Constraints:
- Technical context given to AI:
- Prompt(s):
- AI proposal (summary):
- Decision: ACCEPTED | EDITED | REJECTED
- Rationale:
- Human edits applied:
- Quality gates run: build / unit / integration / lint / security — results
- Validation evidence: <actual command + actual output, test names, pass/fail>
- Risk / follow-up:
- Engineer sign-off:
```

---

### [2026-10-12] T1 — Project scaffold, config, PostgreSQL, Flyway baseline, Compose, health (Task T1)
- Scenario: greenfield
- Intent / problem: Establish a runnable Spring Boot (Java 21) foundation that starts, connects to
  PostgreSQL, runs a Flyway baseline, and serves a health endpoint, with a local Docker Compose
  PostgreSQL and an automated smoke test. No domain logic (T2+).
- Constraints: Java 21; Spring Boot 4.1.1; Maven; PostgreSQL; Flyway; no authentication; no
  office/internal Maven mirror or config in the deliverable; health must not expose DB details to
  unauthenticated callers; minimal dependencies.
- Technical context given to AI: empty greenfield repo; approved T1 plan (Final v3); verified
  environment (Maven 3.10.0 running on JDK 27 → pin JAVA_HOME to 21; Docker client present with
  daemon/compose to be started; Maven Central blocked by the sandbox; several deps uncached).
- Prompt(s): "Proceed with Task T1 only ... show planned structure/deps/steps, wait for approval,
  then implement and run available validation and report actual results." Plus four dependency
  corrections from review: Spring Boot 4.1.1; use spring-boot-starter-webmvc; cache-absence is not a
  downgrade trigger; the only fallback is the current 3.5 maintenance release, explained first.
- AI proposal (summary): pom.xml (SB 4.1.1; webmvc/actuator/jdbc/postgresql/flyway + test/
  testcontainers), application.yml (env datasource, Flyway, actuator health show-details: never),
  V1__baseline.sql (SELECT 1;), docker-compose.yml (postgres:16-alpine), portable
  tools/maven-settings.xml, main class + package-info boundaries, Testcontainers smoke test, README,
  this log.
- Decision: ACCEPTED (plan approved by engineer across three review rounds, with the four
  corrections above applied).
- Rationale: smallest change that proves the build/run/DB/migration/health pipeline end-to-end while
  honoring all locked constraints and keeping modules extractable later.
- Human edits applied: engineer-directed version pin (4.1.1), spring-boot-starter-webmvc, portable
  committed settings, health show-details: never, executable baseline, honest fallback policy.
- Quality gates run (actual, 2026-10-12, after the Maven cache was warmed):
  - Toolchain: PASS — `mvn -v` reports Apache Maven 3.10.0 on Java 21.0.7 (JAVA_HOME pinned to AppleJDK 21).
  - Static checks: PASS — xmllint well-formed (pom.xml, tools/maven-settings.xml); application.yml and
    docker-compose.yml valid YAML.
  - POM model (`mvn validate`): PASS (BUILD SUCCESS) after the Testcontainers BOM fix (see next entry).
  - Compile (`mvn clean compile`): PASS (BUILD SUCCESS) — 3 main sources compiled with release 21.
  - Test-compile (`mvn test-compile`): PASS (BUILD SUCCESS) — test compiles against Testcontainers
    1.21.4 + spring-boot-testcontainers @ServiceConnection at Spring Boot 4.1.1.
  - Automated test (`mvn verify`): BLOCKED (not a code failure) — Testcontainers error
    "Could not find a valid Docker environment" (no Docker daemon). Surefire: "Tests run: 1,
    Failures: 0, Errors: 1" (ContainerFetch for postgres:16-alpine).
  - Compose-PostgreSQL smoke fallback: BLOCKED — same root cause (no Docker daemon; Colima not
    installed; no compose plugin).
  Note: offline builds used -Dmaven.legacyLocalRepo=true to reuse the shared local cache whose
  artifacts were originally fetched under a different repository id; this references no office repo.
- Validation evidence (verbatim):
  - `mvn validate` / `mvn clean compile` / `mvn test-compile`: "BUILD SUCCESS".
  - `mvn verify`: "java.lang.IllegalStateException: Could not find a valid Docker environment" ->
    "[ERROR] UrlShortenerApplicationTests » ContainerFetch Can't get Docker image ... postgres:16-alpine";
    "Tests run: 1, Failures: 0, Errors: 1"; "BUILD FAILURE".
  - No PASS is claimed for the automated test; it is BLOCKED by the missing Docker daemon, not by code.
  - UPDATE (16:20): with Colima running and DOCKER_HOST passed inline, the automated test PASSED
    (Tests run: 2, Failures: 0, Errors: 0) — see the 2026-10-12 16:20 entry below. The BLOCKED status
    above reflected the no-Docker state at that earlier time.
- Risk / follow-up: dependency resolution is now resolved (cache warmed); the only remaining blocker
  for the automated test is a running Docker daemon. To go green: `brew install colima docker-compose
  && colima start`, then `mvn -s tools/maven-settings.xml verify` (add `-o -Dmaven.legacyLocalRepo=true`
  to reuse the cache offline).
- Engineer sign-off: T1 files complete; validate/compile/test-compile PASS; automated test BLOCKED
  solely by the missing Docker daemon (environment, not code). Awaiting engineer review.

---

### [2026-10-12] T1 build fix — Testcontainers BOM (Task T1, AI-assisted debugging)
- Scenario: greenfield (build-error triage)
- Intent / problem: `mvn` reported `'dependencies.dependency.version' for org.testcontainers:postgresql`
  and `...:junit-jupiter is missing`. Root cause: Spring Boot 4.1.1's BOM does not manage Testcontainers
  versions (a change from Boot 3.x, which did).
- Prompt(s): engineer pasted the verbatim Maven error output (POM processing errors, lines 73/79).
- AI proposal (summary): import `org.testcontainers:testcontainers-bom` in `<dependencyManagement>`
  with a pinned `<testcontainers.version>`, so the two test dependencies inherit a managed version.
- Decision: ACCEPTED, with an engineering choice: pinned **1.21.4** (latest stable 1.x) rather than the
  cached 2.0.5, because the test uses the 1.x API (`org.testcontainers.containers.PostgreSQLContainer`,
  `org.testcontainers.junit.jupiter.*`) and 1.x matches Spring Boot's `@ServiceConnection` expectations.
- Rationale: minimal, idiomatic fix; keeps the test API and the Spring Boot integration consistent;
  avoids an unverified jump to the Testcontainers 2.x major.
- Human edits applied: added `<testcontainers.version>1.21.4</testcontainers.version>` and a
  `testcontainers-bom` import to `pom.xml`.
- Quality gates run: PASS — `mvn validate`, `mvn clean compile`, `mvn test-compile` all BUILD SUCCESS
  (offline, cached, `-Dmaven.legacyLocalRepo=true`).
- Validation evidence: the "version is missing" errors no longer appear; `mvn validate` → BUILD SUCCESS;
  `mvn test-compile` → BUILD SUCCESS (test compiles against Testcontainers 1.21.4).
- Risk / follow-up: the 1.21.4 ↔ Boot 4.1.1 pairing is compile-verified; runtime verification is pending
  a Docker daemon. If a runtime Testcontainers/Boot-4 incompatibility appears, re-evaluate (including
  the TC 2.x line) and report before changing.
- Engineer sign-off: <pending review>

---

### [2026-10-12] T1 validation — Docker runtime (Colima) + sandbox limitation (Task T1)
- Scenario: greenfield (environment / validation)
- Intent / problem: run the automated Testcontainers test to green now that Colima is installed.
- Findings (actual): Colima installed and started; socket present at ~/.colima/default/docker.sock;
  docker context `colima` is current. Two distinct blockers observed:
  1. Testcontainers 1.21.4 does not auto-detect the Colima docker context; with DOCKER_HOST unset it
     probed /var/run/docker.sock (absent) and failed with "Could not find a valid Docker environment".
  2. From the Claude Code sandbox, connecting to the Docker socket returns "permission denied" even
     though the socket is owned by the current user. The sandbox denies
     Docker-daemon access. Verified via `docker --context colima info` and explicit DOCKER_HOST — both
     permission denied.
- Decision: the automated test must be run in the engineer's own (non-sandboxed) terminal, with
  DOCKER_HOST pointed at Colima's socket. No project code change required; added a README "Colima" note.
- Remediation (engineer terminal — pass env INLINE so it reaches the Maven/test JVM; a prior `export`
  in a different shell will not apply, which is why a 16:09 run still showed only the UnixSocket and
  DockerDesktop strategies being tried):
    cd ~/ai-assisted-url-shortener
    DOCKER_HOST="unix://$HOME/.colima/default/docker.sock" \
    TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
    JAVA_HOME="$(/usr/libexec/java_home -v 21)" \
    mvn -s tools/maven-settings.xml -o -Dmaven.legacyLocalRepo=true verify
- Quality gates run: validate/compile/test-compile PASS (recorded above). Automated test: BLOCKED from
  the sandbox (Docker socket permission denied); runnable by the engineer via the remediation.
- Validation evidence: "permission denied while trying to connect to the docker API at
  unix://~/.colima/default/docker.sock" (both context and DOCKER_HOST attempts).
- Risk / follow-up: first run pulls postgres:16-alpine via the Colima VM (needs Docker Hub reachable).
- Engineer sign-off: <pending review>

---

### [2026-10-12 16:20] T1 validation — automated test PASS (Task T1)
- Scenario: greenfield (validation)
- Context: engineer ran `mvn verify` in their own (non-sandboxed) terminal with DOCKER_HOST passed
  INLINE to Colima's socket (per the remediation above).
- Result: PASS. Testcontainers started postgres:16-alpine and both tests ran green.
  Evidence (verbatim): "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.31 s --
  in com.urlshortener.UrlShortenerApplicationTests". This verifies Spring context load, real-PostgreSQL
  connectivity, and /actuator/health = UP (contextLoads + healthEndpointReportsStatusUp). NOTE
  (corrected during T2): these T1 tests do NOT query the schema, so they did NOT actually prove Flyway
  ran — Flyway was in fact not executing (missing spring-boot-flyway module; see the T2 Flyway-fix entry).
- Known gap (not code): the `mvn verify` lifecycle then failed at the `package` phase because
  maven-jar-plugin:3.5.1 transitive deps are not cached and the run was offline (`-o`):
  "Cannot access central ... in offline mode and the artifact ... has not been downloaded from it
  before" for commons-io:2.22.0, maven-archiver:3.6.6, plexus-archiver:4.12.0, maven-shared
  file-management:3.2.0.
- Decision / remediation: for a fully green `verify`/`package`, re-run WITHOUT `-o` (online) so these
  build-plugin deps download from Central (the engineer's terminal can reach Central); keep
  -Dmaven.legacyLocalRepo=true. The tests themselves are already verified PASS; packaging is a separate
  build-plugin fetch, and `mvn ... test` alone (offline) also stops after the green tests.
- Quality gates run: unit/integration test PASS (2/2). Full `package` pending one online run.
- Engineer sign-off: tests verified green by engineer on 2026-10-12; final sign-off pending the full
  packaged build.

---

### [2026-10-12] T2 — Core data model + migration (short_link, click_event) (Task T2)
- Scenario: greenfield
- Intent / problem: implement the actual data model — unique short codes, expiration/status fields,
  click events, and the necessary indexes — as a Flyway migration plus JPA entities/repositories, to
  move toward end-to-end create/redirect/analytics.
- Constraints: Flyway owns the schema (Hibernate ddl-auto=none); analytics decoupled from shortening
  (reference by id, not a JPA relationship); PostgreSQL; incremental; run relevant tests.
- Prompt(s): "implement the actual data model and migration for short links and click events,
  including unique short codes, expiration/status fields, and the necessary indexes. Keep changes
  incremental and run the relevant tests."
- Changes:
  - V2__core_schema.sql: short_link (identity id; UNIQUE code; original_url TEXT; status with CHECK in
    (ACTIVE,EXPIRED,DISABLED); is_custom_alias; click_count; created_at; nullable expires_at) and
    click_event (FK -> short_link ON DELETE CASCADE; occurred_at; referrer; user_agent; ip_hash).
    Indexes: UNIQUE(code); partial index on expires_at WHERE NOT NULL; (short_link_id, occurred_at).
  - JPA entities ShortLink (+ LinkStatus enum) and ClickEvent; Spring Data repositories
    (ShortLinkRepository.findByCode/existsByCode; ClickEventRepository.countByShortLinkId).
  - pom: replaced spring-boot-starter-jdbc with spring-boot-starter-data-jpa.
  - application.yml: spring.jpa.hibernate.ddl-auto=none, open-in-view=false, hibernate.jdbc.time_zone=UTC.
  - DataModelPersistenceTest (Testcontainers): V2 recorded, save/find by code, unique-code enforced,
    expiration stored, click events persist.
- Decision rationale: ddl-auto=none keeps Flyway authoritative (no entity/DDL drift); id BY DEFAULT AS
  IDENTITY provides the sequence Base62 code generation (T4) will encode; the partial expires_at index
  keeps expiry sweeps cheap; analytics references short_link_id as a plain Long for module decoupling
  while the DB still enforces the FK.
- Quality gates run (actual): offline compile from the Claude sandbox is BLOCKED —
  spring-boot-starter-data-jpa:4.1.1 is not cached and the sandbox blocks Maven Central ("Cannot access
  central ... in offline mode and the artifact ... has not been downloaded"). The Testcontainers test
  also needs Docker, which the sandbox cannot reach. Build + tests run in the engineer's terminal.
- Validation evidence (VERIFIED 2026-10-12): engineer ran `mvn ... verify` → BUILD SUCCESS. Surefire:
  DataModelPersistenceTest 5/5 and UrlShortenerApplicationTests 2/2 (0 failures/errors); jar packaged.
  Direct Flyway evidence comes from flywayRecordsMigrationV2 (asserts the flyway_schema_history row),
  which passes once the Flyway autoconfig fix (next entry) was applied.
- Risk / follow-up: Instant<->timestamptz and String<->text mappings are exercised by the repo
  round-trip (ddl-auto=none ⇒ no startup validation); if a mapping mismatch surfaces at runtime, adjust
  the entity column mapping.
- Engineer sign-off: <pending run>

---

### [2026-10-12] T2 fix — Flyway auto-configuration module missing (Task T2, AI-assisted debugging)
- Scenario: brownfield (bug surfaced by the new T2 test)
- Intent / problem: DataModelPersistenceTest failed with `ERROR: relation "short_link" does not exist`
  and `relation "flyway_schema_history" does not exist` — Flyway created nothing. The T1 tests masked
  this because they never query the schema (the actuator health check only probes the connection), so
  Flyway had in fact NEVER run since T1.
- Prompt(s): engineer pasted the verbatim Surefire failure (5 errors in DataModelPersistenceTest;
  short_link / flyway_schema_history missing).
- Investigation (actual, from the local cache): spring-boot-autoconfigure-4.1.1 contains NO Flyway
  classes and does NOT list Flyway in META-INF/.../AutoConfiguration.imports; Spring Boot 4 split
  auto-configuration into per-technology modules (spring-boot-jdbc, spring-boot-sql, ... are present).
  Flyway's auto-config lives in a separate module `spring-boot-flyway`, which `flyway-core` does not
  pull in — so migrations never ran.
- Fix: added dependency org.springframework.boot:spring-boot-flyway (version via the BOM).
- Decision rationale: minimal, correct fix restoring Spring Boot's FlywayAutoConfiguration so
  migrations run at startup (before JPA). No schema or application-logic change required.
- Correction to earlier record: the T1 statement "Flyway V1 baseline applied" and the checklist's
  "Flyway records V1 — confirmed indirectly" were WRONG; Flyway had not been running. The T2 test
  flywayRecordsMigrationV2 asserts the flyway_schema_history row directly, so this is now guarded.
- Quality gates run (actual): offline build from the Claude sandbox remains BLOCKED (spring-boot-flyway
  not cached; sandbox blocks Central). Engineer re-run required.
- Validation evidence (VERIFIED 2026-10-12): after adding spring-boot-flyway, engineer ran
  `mvn ... verify` → BUILD SUCCESS; migrations V1 + V2 applied; all tests green
  (DataModelPersistenceTest 5/5 incl. flywayRecordsMigrationV2; UrlShortenerApplicationTests 2/2).
- Engineer sign-off: verified green by engineer on 2026-10-12.

---

### [2026-10-12] T3 — URL validation + unsafe-target checks (Task T3)
- Scenario: greenfield
- Intent / problem: validate destination URLs (absolute http/https with a valid host; reject malformed
  and unsafe schemes; best-effort unsafe-target checks) in a reusable module, without fetching the target.
- Constraints: separate from controllers/DB (reusable by the create API); do NOT fetch/resolve the
  target; do NOT claim complete SSRF protection; follow existing style; no unnecessary dependencies.
- Prompt(s): "T3 only: URL validation and unsafe-target checks ... accept only absolute HTTP/HTTPS with
  a valid host; reject malformed URLs and unsafe schemes (javascript/file/ftp); focused unit tests for
  valid/invalid/unsupported-scheme/missing-host/edge cases; keep validation reusable and decoupled; do
  not fetch destinations; document residual SSRF risks honestly; run tests and report."
- Changes: com.urlshortener.validation — UrlValidator (@Component, pure, no controller/DB deps);
  UrlValidationException + MalformedUrlException (-> HTTP 400) + UnsafeUrlException (-> HTTP 422);
  UrlValidatorTest (33 parameterized cases). No new dependencies (java.net.URI + existing JUnit/AssertJ).
  README: added "Security — URL validation" with honest residual risks.
- Decision rationale: parse with java.net.URI (no protocol handlers, no network) rather than java.net.URL;
  scheme allowlist {http,https}; require non-blank host; reject literal loopback/private/link-local/
  unspecified/CGNAT hosts by string inspection only (no DNS, honoring "do not connect"); 2048-char cap;
  two exception types so the create API can cleanly map 400 vs 422.
- AI-assisted iteration (actual): first `mvn -o -Dtest=UrlValidatorTest test` → "Tests run: 32,
  Failures: 1" — input "data:text/html,<script>alert(1)</script>" threw MalformedUrlException (illegal
  '<'/'>' fail URI parsing) instead of UnsafeUrlException. Engineering judgment: that IS correct
  (illegal chars = malformed); the test categorization was wrong. Refined the test (moved illegal-char
  URLs to the malformed group; used parseable "data:text/plain,hello" for scheme rejection). Re-run →
  "Tests run: 33, Failures: 0, Errors: 0" → BUILD SUCCESS.
- Quality gates run (actual, in the Claude sandbox, offline against the now-cached deps):
  `mvn -o -Dmaven.legacyLocalRepo=true -Dtest=UrlValidatorTest test` → "Tests run: 33, Failures: 0,
  Errors: 0, Skipped: 0, Time elapsed: 0.108 s" → BUILD SUCCESS. These are pure unit tests (no Docker),
  so — unlike the Testcontainers suites — they ran here directly.
- Validation evidence (verbatim): "Running com.urlshortener.validation.UrlValidatorTest / Tests run: 33,
  Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.108 s" then "BUILD SUCCESS".
- Residual risks / limitations (documented in README + UrlValidator javadoc): no DNS means hostnames
  resolving to private IPs and DNS rebinding are not caught; alternative IP encodings (decimal/octal/hex,
  IPv4-mapped IPv6) are not blocked; shorteners are inherently open redirects; IDN/punycode homographs
  are not normalized. Partial SSRF protection only.
- Engineer sign-off: <pending review>

---

### [2026-10-12] T4 — Short-code generation + custom-alias collision handling (Task T4)
- Scenario: greenfield
- Intent / problem: generate unique short codes (Base62 of a DB sequence) and handle custom aliases
  (format validation + collision detection), as reusable services ready for the create-link API (T5).
- Constraints: follow the approved design (Base62 of a DB sequence; UNIQUE constraint as the safety
  net); case-sensitive codes; support custom aliases within scope; existing style; no unnecessary deps.
- Prompt(s): "T4: short-code generation and custom-alias collision handling."
- Changes: V3__short_code_sequence.sql (CREATE SEQUENCE short_code_seq START 62^6 -> 7-char codes);
  shortening.code — Base62 (pure codec), ShortCodeGenerator (@Component; nextval -> Base62),
  CustomAliasValidator (@Component; format + reserved words), ShortCodeService (@Service; generateCode +
  claimCustomAlias), InvalidAliasException (->400), AliasConflictException (->409). Tests: Base62Test,
  CustomAliasValidatorTest (pure), ShortCodeServiceIT (Testcontainers). No new dependencies.
- Decision rationale: a dedicated sequence (not the identity id) lets the code be set BEFORE insert in a
  single statement; nextval is concurrency-safe and monotonic -> generated codes are collision-free by
  construction. Start at 62^6 for consistent 7-char codes. Custom-alias collision handling = pure format
  validation + existsByCode pre-check (friendly 409) + the UNIQUE constraint as the authoritative guard
  for the check-then-insert race (to be mapped to 409 in T5). A generated code equal to an existing
  alias is astronomically rare and caught by the UNIQUE constraint; T5 will regenerate on that violation.
- Quality gates run (actual, in the Claude sandbox, offline/cached — pure tests only):
  `mvn -o -Dmaven.legacyLocalRepo=true -Dtest=Base62Test,CustomAliasValidatorTest test` ->
  "Tests run: 27, Failures: 0, Errors: 0" (Base62Test 11/11, CustomAliasValidatorTest 16/16) -> BUILD SUCCESS.
- Validation evidence (verbatim): "Base62Test ... Tests run: 11, Failures: 0, Errors: 0";
  "CustomAliasValidatorTest ... Tests run: 16, Failures: 0, Errors: 0"; aggregate "Tests run: 27 ...
  BUILD SUCCESS".
- Follow-up (caught during review): the engineer's full `mvn verify` reported "Tests run: 67 ... BUILD
  SUCCESS" but SILENTLY SKIPPED the code-service integration test — it was named ShortCodeServiceIT, and
  the `*IT` suffix is the Failsafe convention which Surefire ignores (Failsafe is not configured). Renamed
  ShortCodeServiceIT -> ShortCodeServiceTest to match the project's existing Testcontainers-test naming
  (e.g. DataModelPersistenceTest). VERIFIED: re-run reported "Tests run: 71, Failures: 0, Errors: 0" ->
  BUILD SUCCESS; ShortCodeServiceTest now runs (4/4). Flyway applied all three migrations
  ("1 - baseline", "2 - core schema", "3 - short code sequence", now at v3) — the direct migration
  evidence the T1 run never produced.
- Residual risks / limitations: sequential Base62 codes are enumerable/guessable (accepted per the
  approved design; to be mitigated by rate limiting in T9, not by obscurity); claimCustomAlias has a
  check-then-insert TOCTOU closed by the UNIQUE constraint at insert time (T5).
- Engineer sign-off: verified green by engineer on 2026-10-12 (71/71, incl. ShortCodeServiceTest 4/4).

---

### [2026-10-12] T5 — Create-link API (POST /api/v1/links) (Task T5)
- Scenario: greenfield
- Intent / problem: expose POST /api/v1/links that validates the URL (T3), resolves a generated code or
  custom alias (T4), persists (T2), returns 201 + Location + body, and maps errors to RFC 7807.
- Constraints: compose the existing modules; RFC 7807 problem responses; existing style; minimal deps.
- Prompt(s): "proceed with T5".
- Changes: shortening.api — CreateLinkRequest, LinkResponse (records), LinkController (POST);
  shortening.service — LinkService (validate -> alias/generated code -> save, with generated-code retry
  and alias-conflict mapping), InvalidExpirationException (->400); platform.web.GlobalExceptionHandler
  (@RestControllerAdvice -> ProblemDetail: MalformedUrl/InvalidAlias/InvalidExpiration -> 400,
  UnsafeUrl -> 422, AliasConflict -> 409); application.yml app.base-url. Tests: LinkControllerTest
  (pure unit), CreateLinkIntegrationTest (RANDOM_PORT + Testcontainers + JDK HttpClient). No new
  dependencies (service-layer validation via UrlValidator/CustomAliasValidator; no @Valid/validation starter).
- Decision rationale: LinkService is NOT @Transactional so each save runs in its own transaction and the
  generated-code retry survives a (rare) unique-constraint collision; the custom-alias check-then-insert
  race is closed by catching DataIntegrityViolationException -> AliasConflictException (409); base-url is
  externalized for building shortUrl.
- Boot-4 findings (AI-assisted debugging): (1) @WebMvcTest/@AutoConfigureMockMvc are no longer in
  spring-boot-test-autoconfigure (the MVC test slice moved to a separate, uncached Boot 4 module) ->
  pivoted the integration test to @SpringBootTest(RANDOM_PORT) + JDK HttpClient (same pattern as T1),
  avoiding that module. (2) Mockito's inline mock-maker cannot self-attach in the Claude sandbox (hard
  error here; only a warning in the engineer's environment) -> rewrote LinkControllerTest to use a
  hand-written stub instead of mock(), so it runs anywhere.
- Quality gates run (actual, in the Claude sandbox, offline/cached): `mvn -o -Dmaven.legacyLocalRepo=true
  -Dtest=LinkControllerTest test` -> "Tests run: 1, Failures: 0, Errors: 0" -> BUILD SUCCESS; full module
  compiles clean.
- Validation evidence: "LinkControllerTest ... Tests run: 1, Failures: 0, Errors: 0" -> BUILD SUCCESS.
  PENDING engineer (Docker): CreateLinkIntegrationTest (5 cases: 201 generated code, 201 custom alias,
  409 duplicate alias, 422 unsafe scheme, 400 malformed) via the DOCKER_HOST-inline `mvn ... verify`.
- Residual risks / limitations: the alias check-then-insert TOCTOU is closed at insert by the UNIQUE
  constraint -> 409; the redirect endpoint (GET /{code}) is T6, so returned shortUrls are not yet
  resolvable; no authentication.
- Engineer sign-off: <pending run>

---

### [2026-10-12] T6+T7+T8 — Redirect, click recording, analytics (consolidated phase)
- Scenario: greenfield (composing existing modules)
- Intent / problem: redirect active links (302); return 404/410 for unknown/expired/deactivated; record
  each successful redirect as a click event + increment the counter consistently; expose analytics
  (aggregate + per-day time series).
- Constraints: reuse existing schema/services (no rewrite); stay within the approved architecture; NO
  queue/Redis/microservices; configurable redirect status; run tests + report; stop after the phase.
- Prompt(s): "Consolidate T6, T7, T8 ... redirect an existing active short link using HTTP 302 ... agreed
  responses for unknown/expired/deactivated ... record each successful redirect as a click event and
  update the click count consistently ... analytics endpoint with aggregate + time-series ... tests."
- Reused (NOT rewritten): ShortLink/ClickEvent entities, ShortLinkRepository.findByCode,
  ClickEventRepository.countByShortLinkId, LinkStatus, GlobalExceptionHandler, app.base-url.
- Changes:
  - ShortLinkRepository: +incrementClickCount (@Modifying atomic update).
  - ClickEventRepository: +findFirst/LastByShortLinkIdOrderByOccurredAt; +findDailyCounts (native
    PostgreSQL per-day group-by, day as ISO string) + DailyClickCountProjection.
  - redirect/: RedirectService (@Transactional resolveAndRecordClick: lookup -> status/expiry checks ->
    increment + insert click in one tx), RedirectController (GET /{code} -> configurable 302),
    ShortLinkGoneException (410); shortening.service.ShortLinkNotFoundException (404, shared).
  - analytics/: AnalyticsService + AnalyticsController (GET /api/v1/links/{code}/analytics);
    AnalyticsResponse + DailyClickCount DTOs.
  - GlobalExceptionHandler: +404 (ShortLinkNotFound), +410 (ShortLinkGone). application.yml:
    app.redirect-status (default 302). No new dependencies; no queue/Redis/microservices.
- Decision rationale: counter increment + click-event insert in ONE @Transactional method => consistency
  with no async (decision #4); atomic SQL increment avoids read-modify-write races; lazy expiry on read
  (status EXPIRED or expiresAt<=now -> 410) with no write on GET (background sweep deferred, P2); total =
  denormalized click_count, first/last via derived queries, time series via a native group-by returning
  the UTC day as an ISO string (avoids JDBC date-mapping ambiguity in projections); redirect status
  externalized (302 default, 301 possible).
- Quality gates run (actual, Claude sandbox): `mvn -o -Dmaven.legacyLocalRepo=true -Dtest=RedirectServiceTest
  test` -> "Tests run: 2, Failures: 0, Errors: 0" -> BUILD SUCCESS (isExpired null/future/past/now; hashIp
  stable hex/null). Whole module compiles (11 test sources).
- Validation evidence: "RedirectServiceTest ... Tests run: 2, Failures: 0, Errors: 0" -> BUILD SUCCESS.
  PENDING engineer (Docker): RedirectIntegrationTest (302 + click recorded; 404 unknown; 410 expired &
  not counted; 410 disabled) and AnalyticsIntegrationTest (totals + today's time-series bucket; zero
  clicks; 404 unknown), plus the T5 CreateLinkIntegrationTest, via the DOCKER_HOST-inline `mvn ... verify`.
- Residual risks / limitations: ipHash uses getRemoteAddr() (no X-Forwarded-For parsing -> behind a proxy
  this is the proxy IP); lazy expiry does not persist the EXPIRED status (background sweep deferred);
  the analytics time-series query is PostgreSQL-specific; GET /{code} at root could shadow a 1-segment
  /actuator index request (health at /actuator/health is unaffected); no auth.
- Engineer sign-off: <pending run>

---

### [2026-10-12] Completeness audit + deliverable packaging (no new features)
- Scenario: brownfield (audit / documentation)
- Prompt(s): "Perform an assignment-completeness audit ... coverage matrix ... do not invent results";
  then review of a reference submission video; then "do 1-6".
- Findings: the core engineering and this AI log were strong; the real gaps were *packaging* — a stale
  README (still described "Task T1"), missing architecture / scenarios / final-summary docs, an
  uncommitted repository, and create/redirect/analytics integration tests that were written but never
  executed. A reference submission confirmed the expected deliverables (architecture, scenarios,
  AI log, setup, summary) and that a UI is an example choice, not a brief requirement.
- Changes (docs only — no code/feature/infra changes, no UI): rewrote README.md (accurate scope, API
  reference, run/demo, testing, security, limitations); added documents/architecture.md,
  documents/scenarios-and-validation.md, documents/engineering-summary.md; added this entry; created the
  initial git commit history.
- Quality gates run (actual): non-Docker suite
  `mvn -o -Dmaven.legacyLocalRepo=true -Dtest=Base62Test,CustomAliasValidatorTest,UrlValidatorTest,LinkControllerTest,RedirectServiceTest test`
  -> "Tests run: 63, Failures: 0, Errors: 0" -> BUILD SUCCESS.
- Validation evidence: 63/63 unit tests verified green in the dev sandbox. The full 86-test `mvn verify`
  (including the Testcontainers create/redirect/analytics integration tests) is pending the engineer's
  Docker run (command in README). No results were invented.
- Engineer sign-off: <pending full verify>
