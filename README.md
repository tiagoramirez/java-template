# Spring Boot Java Template with Hexagonal Architecture

[![Java](https://img.shields.io/badge/Java-25-ED8B00.svg?logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?logo=springboot&logoColor=fff)](#)
[![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=fff)](#)
[![Gradle](https://img.shields.io/badge/Gradle-9.2.0-02303A?logo=gradle&logoColor=fff)](#)

A production-ready Spring Boot template implementing hexagonal architecture (ports & adapters pattern) with comprehensive monitoring, Docker containerization, and 100% test coverage enforcement.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Usage](#usage)
  - [Development Mode](#development-mode)
  - [Pre-Production Mode](#pre-production-mode)
  - [Production Mode](#production-mode)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Documentation](#documentation)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Features

### Core Framework
- **Spring Boot 4.0.2** with Java 25
- **Hexagonal Architecture** (Ports & Adapters pattern)
- **RESTful API** with base path `/api`
- **OpenAPI/Swagger** documentation (SpringDoc 2.8.13)
- **Snake case JSON** convention (configured globally)

### Testing & Quality
- **100% code coverage** requirement enforced by CI
- **JaCoCo** coverage reporting
- **RestAssured** for integration testing
- **JUnit 5** for unit testing
- Separate unit and integration test strategies

### Monitoring & Observability
- **Prometheus** metrics with custom tags
- **Grafana** dashboards with auto-provisioning
- **Spring Boot Actuator** health checks
- Persistent metric storage (30-day retention)
- Pre-configured monitoring stack

### DevOps
- **Docker Compose** multi-service stack
- **GitHub Actions** CI/CD pipeline
- Branch naming enforcement (`feature/*`, `hotfix/*`, `release/*`)
- Automated dependency graph submission
- Gradle wrapper included (9.2.0)

### Development Tools
- **Lombok** for boilerplate reduction
- **Spring DevTools** for hot reload
- Environment-specific profiles (`dev`, `pre-prod`, `prod`)

## Architecture

This project follows **Hexagonal Architecture** principles, organizing code by feature modules with clear separation between business logic and infrastructure concerns.

```
<feature>/
├── adapters/
│   ├── in/web/          # Controllers, DTOs (HTTP layer)
│   └── out/             # External system adapters (DB, APIs)
├── application/         # Services (business logic/use cases)
├── domain/              # Entities, value objects, business rules
└── ports/
    ├── in/              # Input ports (use case interfaces)
    └── out/             # Output ports (dependency interfaces)
```

**Key Principles:**
- Domain layer is isolated from frameworks
- Dependencies point inward (Dependency Inversion)
- Controllers depend on ports, not services directly
- Easy to test, maintain, and swap implementations

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture guide.

## Prerequisites

### Required
- **Java Development Kit (JDK) 25**
- **Docker**

### Optional
- **IDE with Java support**: IntelliJ IDEA, VS Code, Eclipse, etc.
- **Gradle** (wrapper included, no separate installation needed)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/tiagoramirez/java-template.git
cd java-template

# Run in development mode
./gradlew bootRun

# Test the API
curl http://localhost:8080/api/health

# View Swagger documentation
open http://localhost:8080/api/swagger-ui/index.html
```

**Windows users**: Use `gradlew.bat` or just `gradlew` instead of `./gradlew`

## Usage

### Development Mode

Run locally with the `dev` profile for rapid development:

```bash
# Start the application
./gradlew bootRun

# The API will be available at http://localhost:8080
```

**Test endpoints:**
```bash
# Health check
curl http://localhost:8080/api/health

# Swagger UI
open http://localhost:8080/api/swagger-ui/index.html  # macOS/Linux
start http://localhost:8080/api/swagger-ui/index.html # Windows
```

### Pre-Production Mode

Run with Docker Compose including the full monitoring stack (Prometheus + Grafana):

```bash
# 1. Modify environment variables in .env file (optional)
#    Default credentials: admin/admin
#    Change GF_ADMIN_PASSWORD for custom Grafana password

# 2. Start all services
docker compose up -d --build

# 3. Wait ~45 seconds for services to initialize
docker compose ps

# 4. Test the API
curl http://localhost:8080/api/health
```

**Access services:**
- **Application**: http://localhost:8080/api/health
- **Swagger UI**: http://localhost:8080/api/swagger-ui/index.html
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: Internal only (not exposed)

See [docs/MONITORING.md](docs/MONITORING.md) for detailed monitoring guide.

```bash
# Stop all services
docker compose down

# View logs
docker compose logs -f app
```

### Production Mode

For production deployment:

1. **Set environment variables** from `.env` file as system environment variables
2. **Configure profile**: Set `SPRING_PROFILES_ACTIVE=prod`
3. **Start services**: Launch all required infrastructure (database, monitoring, etc.)
4. **Deploy application**: Run Docker container or JAR file
5. **Verify health**: `curl {base_url}/api/health`

**Important for production:**
- Change default Grafana credentials
- Configure proper security settings
- Set up HTTPS/TLS
- Configure external database if needed
- Set up proper logging and alerting

## Testing

### Running Tests

```bash
# Run all tests with coverage
./gradlew test

# Run specific test class
./gradlew test --tests "HealthServiceTest"

# Run specific test method
./gradlew test --tests "HealthServiceTest.check_ShouldReturnHealthStatusWithMessage"

# Run all tests in a package
./gradlew test --tests "com.tiagoramirez.template.health.*"

# Clean build with tests
./gradlew clean build
```

### View Coverage Report

```bash
# After running tests, open the report
open build/reports/jacoco/test/html/index.html   # macOS/Linux
start build\reports\jacoco\test\html\index.html  # Windows
```

### Coverage Requirements

⚠️ **100% code coverage is required** - CI will fail if coverage drops below 100%

**Exclusions** (automatically excluded from coverage):
- Application main class (`Application.java`)
- Constants packages (`**/constants/**`)
- DTOs (`**/*Dto.java`)
- Exception classes (`**/exceptions/**`)

### Testing Strategy

**Unit Tests**: Test classes in isolation with mocked dependencies
- Domain layer: Pure business logic tests
- Service layer: Test use cases with mocked ports
- Controller layer: Test HTTP handling with mocked services

**Integration Tests**: Full request/response cycle testing
- Use `@SpringBootTest` with `RANDOM_PORT`
- RestAssured for API testing
- Test full stack with real Spring context

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed testing guidelines.

## Monitoring

The Docker Compose stack includes a complete monitoring solution:

### Architecture

```
Spring Boot App (port 8080)
    ↓ exposes /api/actuator/prometheus
Prometheus (internal)
    ↓ scrapes metrics every 10s
Grafana (port 3000)
    ↓ visualizes metrics
```

### Available Metrics

- **JVM Metrics**: Memory, threads, GC pauses, class loading
- **HTTP Metrics**: Request count, duration, response times (p50, p95, p99)
- **System Metrics**: CPU usage, process uptime
- **Custom Tags**: Application name and environment on all metrics

### Access Monitoring

```bash
# Start monitoring stack
docker compose up -d

# Access Grafana (default: admin/admin)
open http://localhost:3000

# View raw Prometheus metrics
curl http://localhost:8080/api/actuator/prometheus
```

For detailed monitoring documentation:
- [docs/MONITORING.md](docs/MONITORING.md) - Setup and usage guide
- [docs/VERIFICATION.md](docs/VERIFICATION.md) - Verification steps

## Documentation

### Project Overview
- **[CLAUDE.md](CLAUDE.md)** - AI assistant instructions for working with this codebase
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Hexagonal architecture detailed guide
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines and development setup

### Setup Guides
- **[docs/guides/GITHUB_ACTIONS_SETUP.md](docs/guides/GITHUB_ACTIONS_SETUP.md)** - Complete CI/CD workflow configuration
- **[docs/guides/GITHUB_TAG_PROTECTION_SETUP.md](docs/guides/GITHUB_TAG_PROTECTION_SETUP.md)** - RC tag protection setup

### Monitoring
- **[docs/MONITORING.md](docs/MONITORING.md)** - Monitoring and observability guide
- **[docs/VERIFICATION.md](docs/VERIFICATION.md)** - Monitoring stack verification

## Troubleshooting

### Port 8080 already in use

**Problem**: Application fails to start with "Address already in use" error

**Solution**:
```bash
# Find and kill process using port 8080
# Linux/macOS
lsof -ti:8080 | xargs kill -9

# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in application.yml
server:
  port: 8081
```

### Docker container fails to start

**Problem**: Container exits immediately or won't start

**Solution**:
```bash
# Check logs for specific error
docker compose logs app

# Common fixes:
# 1. Ensure JDK 25 is being used
# 2. Check for port conflicts
# 3. Verify .env file exists and is properly formatted
# 4. Try clean rebuild
docker compose down -v
docker compose up -d --build
```

### JDK version mismatch

**Problem**: Build fails with "unsupported class file version"

**Solution**:
```bash
# Verify Java version
java -version  # Should show version 25

# Set JAVA_HOME environment variable
export JAVA_HOME=/path/to/jdk-25  # Linux/macOS
set JAVA_HOME=C:\path\to\jdk-25   # Windows

# Or use IDE's JDK configuration
```

### Gradle build fails

**Problem**: Dependency resolution or build errors

**Solution**:
```bash
# Clean and refresh dependencies
./gradlew clean build --refresh-dependencies

# If permission denied (Linux/macOS)
chmod +x gradlew
./gradlew build
```

### Coverage report fails on Windows

**Problem**: Can't find coverage report file

**Solution**:
```bash
# Use backslashes for Windows paths
start build\reports\jacoco\test\html\index.html

# Or navigate manually to:
# build\reports\jacoco\test\html\index.html
```

### Grafana shows "No Data"

**Problem**: Dashboards display "No Data" or empty panels

**Solution**:
1. Wait 2-3 minutes after startup for metrics to populate
2. Adjust time range to "Last 15 minutes" in Grafana
3. Verify datasource: Configuration → Data Sources → Prometheus → Test
4. Check Prometheus is scraping: `docker logs prometheus`
5. Verify app is exposing metrics: `curl http://localhost:8080/api/actuator/prometheus`

See [docs/MONITORING.md](docs/MONITORING.md) for more troubleshooting steps.

### Grafana datasource issues

**Problem**: Datasource not provisioning correctly

**Solution**: Datasource auto-provisions on startup. If issues persist:
```bash
# Restart Grafana
docker compose restart grafana

# Check provisioning logs
docker compose logs grafana | grep -i provision

# Verify datasource exists
curl -u admin:admin http://localhost:3000/api/datasources
```

See [docs/VERIFICATION.md](docs/VERIFICATION.md) for detailed verification steps.

### Tests fail with coverage below 100%

**Problem**: JaCoCo fails build due to insufficient coverage

**Solution**:
1. Check coverage report: `build/reports/jacoco/test/html/index.html`
2. Identify uncovered lines (highlighted in red)
3. Add missing unit tests for services and adapters
4. Ensure DTOs end with `Dto.java` suffix (auto-excluded)
5. Place exceptions in `exceptions/` package (auto-excluded)
6. Place constants in `constants/` package (auto-excluded)

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Branch naming conventions (`feature/*`, `hotfix/*`, `release/*`)
- Commit message guidelines
- Pull request process
- Code style requirements
- Testing requirements

### Quick Contribution Steps

1. **Fork** the repository
2. **Create branch**: `git checkout -b feature/your-feature-name`
3. **Make changes** following hexagonal architecture
4. **Add tests** ensuring 100% coverage
5. **Commit**: `git commit -m "feat: add your feature"`
6. **Push**: `git push origin feature/your-feature-name`
7. **Create Pull Request** to `develop` branch

## License

This is a template repository. You can use it freely for your own projects. Consider adding your own license file.

---

**Built with ❤️ using Spring Boot, Hexagonal Architecture, and modern DevOps practices**
