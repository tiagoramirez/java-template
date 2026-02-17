# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.2 application using Java 25 with hexagonal (ports & adapters) architecture. Template project featuring RESTful API, monitoring stack (Prometheus + Grafana), Docker containerization, and 100% test coverage requirement.

## Architecture

### Hexagonal Architecture Pattern

Code is organized by feature modules using hexagonal architecture:

```
com.tiagoramirez.template.<feature>/
├── adapters/
│   ├── in/web/          # Controllers, DTOs for HTTP layer
│   └── out/             # External system adapters
├── application/         # Service layer (use cases/business logic)
├── domain/              # Domain entities and logic
└── ports/
    ├── in/              # Inbound ports (interfaces for use cases)
    └── out/             # Outbound ports (interfaces for external dependencies)
```

**Key principles:**
- Domain layer is isolated from frameworks and external concerns
- Controllers depend on input ports, not directly on services
- Services implement input ports and depend on output ports
- Adapters implement output ports
- Dependencies point inward (Dependency Inversion)

Example: `health` feature demonstrates the pattern with HealthController → HealthPort → HealthService → TimeProviderPort → TimeProviderAdapter.

### Configuration

- **AppConfig.java**: Global beans (ObjectMapper with snake_case, OpenAPI, Prometheus common tags)
- **Profiles**: `dev`, `pre-prod`, `prod` (controlled via `SPRING_PROFILES_ACTIVE`)
- **Base path**: All endpoints prefixed with `/api`
- **Actuator endpoints**: `/api/actuator/health`, `/api/actuator/prometheus`

## Common Commands

> **Note**: Use `./gradlew` on macOS/Linux or `gradlew.bat` (or just `gradlew`) on Windows.

### Build & Test

```bash
# Build project
./gradlew build

# Run tests with coverage (requires 100% coverage)
./gradlew test

# Clean build with refreshed dependencies
./gradlew clean build --refresh-dependencies

# View coverage report
# macOS/Linux
open build/reports/jacoco/test/html/index.html
# Windows
start build\reports\jacoco\test\html\index.html
```

### Running the Application

```bash
# Dev mode (local development)
./gradlew bootRun

# Pre-prod mode (Docker with monitoring stack)
docker compose up -d --build

# Stop services
docker compose down

# View logs
docker compose logs -f app
```

### Testing Endpoints

```bash
# Health check
curl http://localhost:8080/api/health

# Prometheus metrics
curl http://localhost:8080/api/actuator/prometheus

# Swagger UI
# macOS/Linux
open http://localhost:8080/api/swagger-ui/index.html
# Windows
start http://localhost:8080/api/swagger-ui/index.html
```

### Single Test Execution

```bash
# Run single test class
./gradlew test --tests "com.tiagoramirez.template.health.application.HealthServiceTest"

# Run single test method
./gradlew test --tests "com.tiagoramirez.template.health.application.HealthServiceTest.check_ShouldReturnHealthStatusWithMessage"

# Run all tests in package
./gradlew test --tests "com.tiagoramirez.template.health.*"
```

## Environment Configuration

### .env File

Used for Docker Compose deployments (pre-prod/prod profiles):
- Contains Grafana credentials (`GF_ADMIN_USER`, `GF_ADMIN_PASSWORD`)
- Modify before running `docker compose up` for custom configurations
- **Default credentials**: admin/admin
- **Production**: Change default credentials for security

### Environment Variables

Key environment variables:
- `SPRING_PROFILES_ACTIVE`: Set to `dev`, `pre-prod`, or `prod`
- `JAVA_HOME`: Must point to JDK 25

## Testing Strategy

- **Unit tests**: Test individual classes in isolation (e.g., `HealthServiceTest`)
- **Integration tests**: Test full request/response cycle with `@SpringBootTest` and RestAssured (e.g., `HealthTest`)
- **Coverage**: JaCoCo configured with 100% coverage requirement in CI
- **Exclusions**: Application class, constants, DTOs, and exceptions excluded from coverage

### Writing Tests

- Integration tests use `@ActiveProfiles("test")` and `SpringBootTest.WebEnvironment.RANDOM_PORT`
- RestAssured is configured in `@BeforeEach` with dynamic port binding
- Unit tests mock dependencies using constructor injection

## Key Dependencies

- **Spring Boot**: 4.0.2
- **Java**: 25 (required)
- **Lombok**: Reduces boilerplate (requires annotation processor in IDE)
- **SpringDoc OpenAPI**: 2.8.13 (Swagger UI at `/api/swagger-ui/index.html`)
- **Micrometer Prometheus**: Metrics registry for monitoring
- **RestAssured**: 6.0.0 (integration testing)
- **JaCoCo**: 0.8.14 (code coverage reporting)
- **Gradle**: 9.2.0 (wrapper included)

## Docker & Monitoring

The docker-compose stack includes:
- **app**: Spring Boot application (port 8080)
- **prometheus**: Metrics scraping (internal, scrapes every 10s)
- **grafana**: Dashboards (port 3000, auto-configured with datasource and dashboards)

Monitoring details in `docs/MONITORING.md` and `docs/VERIFICATION.md`.

## CI/CD

GitHub Actions workflow (`.github/workflows/pre-merge-validation.yml`) enforces:

### Branch Workflow
- **`feature/*` branches** → merge to `develop`
  - Example: `feature/add-authentication`
- **`hotfix/*` branches** → merge to `develop`
  - Example: `hotfix/fix-npe`
- **`release/x.y.z` branches** → merge to `main` (semantic versioning)
  - Example: `release/1.2.0`

### CI Pipeline
- **Branch name validation**: PRs rejected if branch naming doesn't match convention
- **Java 25 + Gradle 9.2.0** with build caching
- **Test execution**: `./gradlew clean test --no-daemon`
- **100% coverage requirement**: Build fails if coverage < 100%
- **Artifacts**: Coverage report uploaded to GitHub Actions
- **Dependency graph**: Submitted for security scanning

## JaCoCo Coverage Exclusions

The following patterns are excluded from coverage reports:
- `com/tiagoramirez/*/Application*`
- `com/tiagoramirez/**/constants/**`
- `com/tiagoramirez/**/**/Dto.java`
- `com/tiagoramirez/**/exceptions/**`

When adding new features, ensure DTOs, constants, and exception classes follow these naming conventions to be automatically excluded.

## Adding New Features

When creating a new feature module:

1. **Package naming**: `com.tiagoramirez.template.<feature>/`
   - Use lowercase, singular nouns (e.g., `health`, `order`, `payment`)
2. **Structure** using hexagonal architecture (see pattern above)
3. **DTOs**: Must end with `Dto.java` suffix for coverage exclusion
4. **Exceptions**: Place in `<feature>/exceptions/` package for exclusion
5. **Constants**: Place in `<feature>/constants/` package for exclusion
6. Add unit tests for services/adapters
7. Add integration tests with `@SpringBootTest` and RestAssured
8. Ensure 100% coverage (excluding DTOs, exceptions, constants)
9. Add custom metrics if needed (Counter, Timer, Gauge - see [MONITORING.md](docs/MONITORING.md))

### Example Feature Structure

```
com.tiagoramirez.template.user/
├── adapters/
│   ├── in/
│   │   └── web/
│   │       ├── UserController.java        # REST controller
│   │       ├── CreateUserRequestDto.java  # Request DTO (excluded)
│   │       └── UserResponseDto.java       # Response DTO (excluded)
│   └── out/
│       └── UserRepositoryAdapter.java     # External adapter
├── application/
│   └── UserService.java                   # Business logic
├── domain/
│   └── User.java                          # Domain entity
├── exceptions/
│   └── UserNotFoundException.java         # Custom exception (excluded)
├── constants/
│   └── UserConstants.java                 # Constants (excluded)
└── ports/
    ├── in/
    │   └── CreateUserPort.java            # Use case interface
    └── out/
        └── UserRepositoryPort.java        # Dependency interface
```

## Troubleshooting

### Port 8080 already in use
Modify port in `application.yml` or stop conflicting service:
```bash
# Linux/macOS
lsof -ti:8080 | xargs kill -9
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Docker container fails to start
Check logs: `docker compose logs app`
Common fixes:
- Ensure JDK 25 is being used
- Check for port conflicts
- Verify `.env` file exists and is properly formatted
- Try clean rebuild: `docker compose down -v && docker compose up -d --build`

### JDK version mismatch
Ensure `JAVA_HOME` points to JDK 25:
```bash
java -version  # Should show version 25
# Linux/macOS
export JAVA_HOME=/path/to/jdk-25
# Windows
set JAVA_HOME=C:\path\to\jdk-25
```

### Gradle build fails
Run `./gradlew clean build --refresh-dependencies`

### Coverage report fails with Windows paths
Use backslashes: `start build\reports\jacoco\test\html\index.html`

### Grafana shows "No Data"
1. Wait 2-3 minutes after startup for metrics to populate
2. Adjust time range to "Last 15 minutes"
3. Verify datasource: Configuration → Data Sources → Prometheus → Test
4. Check Prometheus scraping: `docker logs prometheus`
5. Verify metrics endpoint: `curl http://localhost:8080/api/actuator/prometheus`

### Grafana datasource issues
Datasource auto-provisions on startup. If issues persist:
```bash
docker compose restart grafana
docker compose logs grafana | grep -i provision
```
See [VERIFICATION.md](docs/VERIFICATION.md) for detailed verification steps.

### Tests fail with coverage below 100%
1. Check coverage report: `build/reports/jacoco/test/html/index.html`
2. Identify uncovered lines (highlighted in red)
3. Add missing unit tests for services and adapters
4. Ensure DTOs end with `Dto.java` suffix (auto-excluded)
5. Place exceptions in `exceptions/` package (auto-excluded)
6. Place constants in `constants/` package (auto-excluded)
