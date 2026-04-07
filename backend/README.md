# Pretzel Shop API (Spring Boot)

Java 21 rewrite of the **public-pretzel-shop-be** Node.js API: same routes, PostgreSQL schema (Liquibase), Redis cart sessions, and JSON shapes aligned with the original service.

## Prerequisites

- **Java 21** (recommended via [SDKMAN](https://sdkman.io/))
- **Docker** and Docker Compose (for PostgreSQL and Redis)
- This project uses the **Gradle Wrapper** (`./gradlew`); a global Gradle install is optional.

### Install Java 21 and Gradle with SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 21.0.5-tem
sdk default java 21.0.5-tem

# Optional: global Gradle (the wrapper is enough for builds)
sdk install gradle 8.12
```

Confirm:

```bash
java -version   # should report 21.x
./gradlew -version
```

If `gradlew` is not executable after cloning, run: `chmod +x gradlew`.

## Configuration

Environment variables match the original Node backend (defaults shown):

| Variable | Default |
|----------|---------|
| `PORT` | `3001` |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `pretzel_shop` |
| `DB_USER` | `pretzel_user` |
| `DB_PASSWORD` | `pretzel_password` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `CORS_ORIGIN` | `http://localhost:5173` |

## Run locally

1. Start PostgreSQL and Redis:

   ```bash
   docker compose up -d
   ```

2. Run the application (Liquibase applies schema and seeds products when the DB is empty):

   ```bash
   ./gradlew bootRun
   ```

3. Health check: `GET http://localhost:3001/health`

## API overview

- `GET /health`
- `GET /api/products`, `GET /api/products/{id}`
- `GET|POST|PUT|DELETE /api/cart` (session via `X-Session-Id`, exposed on responses; cart TTL 24 hours in Redis)
- `POST /api/orders`, `GET /api/orders/{id}`

## Tests and coverage

```bash
./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

- HTML report: `build/reports/jacoco/test/html/index.html`
- Tests use an in-memory H2 database and a mocked Redis `StringRedisTemplate` (`src/test/resources/application-test.yml`).

## Tech stack

- Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Spring Data Redis
- PostgreSQL driver, Liquibase
- JUnit 5, Mockito, AssertJ, Spring MockMvc
- JaCoCo (line coverage gate at 50% on `check`)
