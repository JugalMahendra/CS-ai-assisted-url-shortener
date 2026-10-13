<div align="center">

# 🔗 AI-Assisted URL Shortener

**Short links · 302 redirects · click analytics** — a production-style service built in
**Java 21 / Spring Boot 4.1** with **PostgreSQL + Flyway**.
Engineer-led, AI-accelerated, with full traceability.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-V1–V3-CC0200?logo=flyway&logoColor=white)
![Tests](https://img.shields.io/badge/tests-JUnit%205%20%2B%20Testcontainers-25A162?logo=junit5&logoColor=white)
![API](https://img.shields.io/badge/API-OpenAPI%203.1-6BA539?logo=openapiinitiative&logoColor=white)

📐 [Architecture](documents/architecture.md) &nbsp;·&nbsp; 🎬 [Scenarios &amp; Validation](documents/scenarios-and-validation.md) &nbsp;·&nbsp; 📋 [Engineering Summary](documents/engineering-summary.md) &nbsp;·&nbsp; 🤖 [AI Engineering Log](docs/ai-engineering-log.md)

</div>

---

## ✨ Features

| | |
|---|---|
| 🔗 **Create** | 7-char Base62 codes from a DB sequence (collision-free) or a custom alias — `POST /api/v1/links` |
| ↪️ **Redirect** | `GET /{code}` → **302** to the original URL (status configurable) |
| 📊 **Analytics** | total clicks, first/last click, and a per-day time series |
| 🧮 **Consistent counts** | each redirect records a click **+** increments the counter in **one transaction** (no queue) |
| ⏳ **Expiry / deactivation** | expired or disabled links → **410**; unknown → **404** |
| 🛡️ **URL safety** | http/https only, host checks, literal private/loopback blocking (honest *partial* SSRF guard) |
| ❤️ **Health** | `GET /actuator/health` |
| 📜 **OpenAPI** | Swagger UI at `/docs/index.html` |

---

## 🚀 Quick start

> **Prerequisites:** Java 21 · Maven 3.9+ · a Docker runtime (Colima / Docker Desktop / OrbStack)

```bash
# 1 — start PostgreSQL (Compose, or a plain docker run)
docker compose up -d db
#   or:
docker run -d --name urlshortener-db \
  -e POSTGRES_DB=urlshortener -e POSTGRES_USER=urlshortener -e POSTGRES_PASSWORD=urlshortener \
  -p 5432:5432 postgres:16-alpine

# 2 — run the app (Flyway applies V1–V3 on startup)
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn spring-boot:run
```

**Try it (second terminal):**

```bash
# create a short link
curl -s -X POST localhost:8080/api/v1/links \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.example.com/some/long/path"}'
# → {"code":"1000000","shortUrl":"http://localhost:8080/1000000", ... ,"status":"ACTIVE"}

CODE=$(curl -s -X POST localhost:8080/api/v1/links -H 'Content-Type: application/json' \
  -d '{"url":"https://www.example.com/"}' | sed -n 's/.*"code":"\([^"]*\)".*/\1/p')

curl -i "localhost:8080/$CODE"                        # 302 + Location  (the redirect)
for i in 1 2 3; do curl -s -o /dev/null "localhost:8080/$CODE"; done
curl -s "localhost:8080/api/v1/links/$CODE/analytics" # totals + per-day series
curl -s localhost:8080/actuator/health                # {"status":"UP"}
```

Stop the app: `lsof -ti :8080 -sTCP:LISTEN | xargs kill`

---

## 📖 API

| Method & path | Purpose | Success | Errors |
|---|---|---|---|
| `POST /api/v1/links` | create a short link | `201` + `Location` + link JSON | `400` invalid URL/alias/expiry · `422` unsafe URL · `409` alias taken |
| `GET /{code}` | redirect | `302` + `Location` | `404` unknown · `410` expired/disabled |
| `GET /api/v1/links/{code}/analytics` | analytics | `200` analytics JSON | `404` unknown |
| `GET /actuator/health` | health | `200` `{"status":"UP"}` | — |

**Request:** `{"url":"<required http/https>","customAlias":"<optional>","expiresAt":"<optional ISO-8601, future>"}`
**Errors:** RFC 7807 `application/problem+json` → `{type, title, status, detail}`

### 🧭 Interactive docs (Swagger UI)
With the app running, open **<http://localhost:8080/docs/index.html>** (OpenAPI 3.1).
Raw spec: **`/docs/openapi.yaml`** (also at `src/main/resources/static/docs/openapi.yaml`) — imports into Postman / Swagger Editor.
It's a hand-maintained static spec (no runtime dependency); **Try it out** calls the live API. Served under `/docs/` because the redirect endpoint owns the root `/{code}` path.

---

## ⚙️ Configuration

All settings are environment-overridable (defaults match `docker-compose.yml`):

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/urlshortener` |
| `DB_USERNAME` / `DB_PASSWORD` | `urlshortener` / `urlshortener` |
| `SERVER_PORT` | `8080` |
| `APP_BASE_URL` | `http://localhost:8080` |
| `APP_REDIRECT_STATUS` | `302` (set `301` for permanent) |

> ⚠️ Datasource defaults are **local-dev values only** (a disposable local container) — **not production
> secrets**. In any real environment, inject `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` via environment
> variables or a secrets manager; the app reads them from the environment first.

---

## 🧪 Testing

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn clean verify
```

**86 tests** — unit (Base62, alias rules, URL validation, controller mapping, redirect helpers) **+**
Testcontainers integration (schema/migrations, code generation, create/redirect/analytics over real
HTTP + PostgreSQL).

> **Verification status (honest):** the 63 unit tests pass without Docker; the Testcontainers suite
> runs against real PostgreSQL via `mvn verify`. See [Engineering Summary](documents/engineering-summary.md#testing-approach--honest-status) for exactly what's been observed green.

<details>
<summary>🐳 Running the tests with Colima (macOS)</summary>

Testcontainers needs `DOCKER_HOST`; pass it inline so it reaches the test JVM:
```bash
DOCKER_HOST="unix://$HOME/.colima/default/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn verify
```
Docker Desktop / OrbStack users don't need these. If Ryuk errors on the socket mount, add `TESTCONTAINERS_RYUK_DISABLED=true`.
</details>

---

## 🔒 Security

`UrlValidator` is reusable (no controller/DB coupling) and **never fetches, resolves DNS, or connects** to the target.

- ✅ **Accepted:** absolute `http`/`https` with a valid host, ≤ 2048 chars.
- 🚫 **400 (malformed):** unparseable / illegal chars / non-absolute / missing host / over-length.
- 🚫 **422 (unsafe):** non-http(s) schemes (`javascript:`, `file:`, `ftp:`, `data:`, `mailto:`) and literal
  loopback/private/link-local/CGNAT hosts (`localhost`, `127/8`, `10/8`, `172.16/12`, `192.168/16`,
  `169.254/16` incl. cloud metadata, `0.0.0.0`, `::1`, `fe80::/10`, `fc00::/7`).

> **Residual risks (partial protection — stated plainly):** hostnames that *resolve* to private IPs and
> DNS rebinding aren't caught (no DNS by design); alternative IP encodings (decimal/octal/hex, IPv4-mapped
> IPv6) aren't blocked; a shortener is inherently an open redirect; IDN/punycode homographs aren't
> normalized. These belong to the redirect layer + network-egress controls, not validation alone.

---

## 🏗️ Architecture

A lean **modular monolith** (no queue, cache, or extra services) — simple consistency (one DB transaction
per redirect) with clean module boundaries. Full write-up + diagram in
**[`documents/architecture.md`](documents/architecture.md)**.

```
shortening → create, Base62/sequence code-gen, aliases      validation → URL + unsafe-target checks
redirect   → 302 + transactional click recording            analytics  → aggregate + per-day series
platform   → RFC 7807 error mapping, health                 (PostgreSQL + Flyway V1–V3)
```

---

## 📂 Project structure

```
pom.xml  docker-compose.yml  tools/maven-settings.xml      # build · local DB · portable (office-free) settings
documents/                   architecture · scenarios & validation · engineering summary
docs/ai-engineering-log.md   AI-assisted traceability log
src/main/java/com/urlshortener/
  shortening/  redirect/  analytics/  validation/  platform/   # feature + cross-cutting modules
src/main/resources/
  application.yml
  db/migration/              V1 baseline · V2 core schema · V3 short-code sequence
  static/docs/               OpenAPI spec + Swagger UI
src/test/java/com/urlshortener/                                # unit + Testcontainers integration
```

---

## ⚠️ Limitations & future scope

No authentication · no rate limiting · single-node · synchronous click recording · lazy expiry (no
background sweep) · PostgreSQL-specific analytics query · app runs via `mvn`/JAR (not containerized).
Rationale and a full future-scope list are in the
**[Engineering Summary](documents/engineering-summary.md#limitations--future-scope)**.

<details>
<summary>🌐 Building on restricted networks (Maven Central)</summary>

Dependencies resolve from **public Maven Central only** (no corporate/internal mirror). On a machine
whose `~/.m2/settings.xml` forces a mirror, add `-s tools/maven-settings.xml` (committed, no
credentials/URLs). Offline after a one-time warm-up:
```bash
mvn -s tools/maven-settings.xml dependency:go-offline
mvn -s tools/maven-settings.xml -o verify
```
</details>

<div align="center"><sub>Built as an AI-assisted engineering exercise · AI accelerates, the engineer owns correctness.</sub></div>
