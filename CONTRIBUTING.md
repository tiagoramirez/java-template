# Contributing to Java Spring Boot Template

Thank you for your interest in contributing! This is a **template repository** designed to help developers bootstrap Spring Boot applications with hexagonal architecture, Docker containerization, and production-ready monitoring.

We welcome contributions in the form of bug reports, feature requests, documentation improvements, and pull requests.

---

## Table of Contents

- [How to Contribute](#how-to-contribute)
- [Branch Naming Convention](#branch-naming-convention)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Testing Requirements](#testing-requirements)
- [Code Style](#code-style)
- [Pull Request Process](#pull-request-process)
- [Development Setup](#development-setup)
- [Questions or Problems?](#questions-or-problems)

---

## How to Contribute

### Issues

- **Bug Reports**: If you find a bug, please open an issue with:
  - Clear description of the problem
  - Steps to reproduce
  - Expected vs. actual behavior
  - Environment details (Java version, OS, etc.)

- **Feature Requests**: Suggest new features that would benefit the template:
  - Describe the use case
  - Explain why it's valuable for a template project

- **Questions**: Open an issue for questions about usage or architecture

### Pull Requests

Pull requests are the best way to propose changes:
- Fix bugs
- Add new features
- Improve documentation
- Enhance tests

---

## Branch Naming Convention

⚠️ **CRITICAL**: Branch naming is **enforced by CI/CD**. Pull requests with incorrect branch names will be automatically rejected.

### Rules

| Base Branch | Allowed Source Branches | Example               | Purpose                    |
|-------------|-------------------------|------------------------|----------------------------|
| `develop`   | `feature/*`             | `feature/add-logging` | New features and enhancements |
| `develop`   | `hotfix/*`              | `hotfix/fix-npe`      | Critical bug fixes         |
| `main`      | `release/*`             | `release/1.2.3`       | Production releases (semantic versioning) |

### Examples

✅ **Valid**:
```bash
git checkout -b feature/add-user-authentication
git checkout -b feature/improve-monitoring
git checkout -b hotfix/fix-health-endpoint
git checkout -b release/1.0.0
```

❌ **Invalid**:
```bash
git checkout -b add-feature           # Missing feature/ prefix
git checkout -b bugfix/fix-something  # Use hotfix/ instead
git checkout -b release/v1.0.0        # Don't use 'v' prefix
```

### Enforcement

CI validates branch names in `.github/workflows/pre-merge-validation.yml`:
- PRs to `develop` must come from `feature/*` or `hotfix/*` branches
- PRs to `main` must come from `release/X.Y.Z` or `hotfix/*` branches

---

## Commit Message Guidelines

We recommend using [Conventional Commits](https://www.conventionalcommits.org/) format (optional but helpful):

### Format

```
<type>: <description>

[optional body]

[optional footer]
```

### Types

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `refactor:` Code refactoring (no functional change)
- `test:` Adding or updating tests
- `chore:` Build/tooling changes
- `style:` Code style changes (formatting)
- `perf:` Performance improvements

### Examples

```bash
feat: add JWT authentication support
fix: resolve NPE in HealthController
docs: update README with Docker Compose usage
test: add integration tests for health endpoint
refactor: extract user validation to separate service
```

### Rules

- Keep first line under 72 characters
- Use imperative mood ("add" not "added" or "adds")
- Don't capitalize first letter after colon
- No period at the end of subject line

---

## Testing Requirements

⚠️ **CRITICAL**: **100% code coverage is required**. CI will fail if coverage drops below 100%.

### Coverage Configuration

JaCoCo enforces coverage with exclusions for:
- Application main class: `Application.java`
- Constants: `**/constants/**`
- DTOs: `**/*Dto.java`
- Exceptions: `**/exceptions/**`

See `build.gradle` lines 62-69 for exclusion patterns.

### What to Test

#### Unit Tests

Test each class in isolation:

**Domain Layer**:
```java
// Test business rules
class HealthStatusTest {
    @Test
    void ok_ShouldCreateHealthyStatus() {
        // Test domain logic
    }
}
```

**Service Layer**:
```java
// Mock output ports
class HealthServiceTest {
    @Mock
    private TimeProviderPort timeProvider;

    @InjectMocks
    private HealthService service;

    @Test
    void check_ShouldReturnHealthStatus() {
        // Test service with mocked dependencies
    }
}
```

**Controller Layer**:
```java
// Mock input ports
class HealthControllerTest {
    @Mock
    private HealthPort healthPort;

    @InjectMocks
    private HealthController controller;

    @Test
    void check_ShouldReturn200() {
        // Test controller logic
    }
}
```

#### Integration Tests

Test full request/response cycle:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthTest {
    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    @Test
    void health_ShouldReturn200WithMessage() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body("message", equalTo("I'm alive!"));
    }
}
```

### Running Tests Locally

```bash
# Run all tests with coverage
./gradlew test

# Run specific test class
./gradlew test --tests "HealthServiceTest"

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Coverage Requirements

- **Target**: 100% (enforced by CI for `feature/*` and `release/*` branches)
- **Location**: `.github/workflows/pre-merge-validation.yml`
- CI will fail if coverage < 100% (hotfix branches exempt for emergency fixes)
- Ensure new code is fully covered before submitting PR

---

## Code Style

### Hexagonal Architecture

Follow the established pattern:

```
<feature>/
├── adapters/
│   ├── in/web/          # Controllers, DTOs
│   └── out/             # External adapters
├── application/         # Services (use cases)
├── domain/              # Entities, value objects
└── ports/
    ├── in/              # Input port interfaces
    └── out/             # Output port interfaces
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed guidance.

### Dependency Injection

Use **constructor injection** (not field injection):

✅ **Good**:
```java
@Component
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

❌ **Bad**:
```java
@Component
public class UserService {
    @Autowired
    private UserRepository repository;  // Don't use field injection
}
```

### Lombok

Use Lombok to reduce boilerplate:
- `@RequiredArgsConstructor` for constructor injection
- Records for DTOs and domain entities where appropriate
- Avoid `@Data` (too permissive)

### JSON Naming Convention

Use **snake_case** for JSON fields (configured in `AppConfig.java`):

```java
// Java: camelCase
public record UserResponseDto(String firstName, String lastName) {}

// JSON: snake_case
{
  "first_name": "John",
  "last_name": "Doe"
}
```

### Package Structure

Organize by **feature**, not by layer:

✅ **Good**:
```
com.tiagoramirez.template/
├── health/
│   ├── adapters/
│   ├── application/
│   ├── domain/
│   └── ports/
└── user/
    ├── adapters/
    ├── application/
    ├── domain/
    └── ports/
```

❌ **Bad**:
```
com.tiagoramirez.template/
├── controllers/
├── services/
├── repositories/
└── models/
```

---

## Pull Request Process

### 1. Fork and Clone

```bash
# Fork repository on GitHub
git clone https://github.com/<your-username>/java-template.git
cd java-template
```

### 2. Create Feature Branch

```bash
# Follow branch naming convention
git checkout -b feature/your-feature-name
```

### 3. Make Changes

- Write code following [Code Style](#code-style)
- Follow hexagonal architecture pattern
- Add unit and integration tests

### 4. Test Locally

```bash
# Ensure all tests pass and coverage is 100%
./gradlew clean build

# Check coverage report
open build/reports/jacoco/test/html/index.html
```

### 5. Commit Changes

```bash
# Use conventional commits format
git add .
git commit -m "feat: add your feature description"
```

### 6. Push and Create PR

```bash
# Push to your fork
git push origin feature/your-feature-name
```

Create PR on GitHub:
- Clear title describing the change
- Description explaining what and why
- Reference any related issues

### 7. Wait for CI Checks

CI will automatically run:
1. **Branch name validation** (must follow convention)
2. **Tests** (all must pass)
3. **Coverage check** (must be 100%)

If any check fails, fix the issues and push updates.

### 8. Address Review Comments

- Respond to reviewer feedback
- Push additional commits as needed
- Keep discussion respectful and constructive

---

## Development Setup

### Prerequisites

- **Java 25**
- **Docker**
- **IDE**: IntelliJ IDEA, VS Code, Eclipse, etc.

### Initial Setup

```bash
# Clone repository
git clone https://github.com/<your-username>/java-template.git
cd java-template

# Run application (dev profile)
./gradlew bootRun

# Test API
curl http://localhost:8080/api/health

# Swagger UI
open http://localhost:8080/api/swagger-ui/index.html
```

### Docker Development

```bash
# Start full stack (app + Prometheus + Grafana)
docker compose up -d --build

# View logs
docker compose logs -f

# Stop services
docker compose down
```

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "HealthServiceTest"

# Specific test method
./gradlew test --tests "HealthServiceTest.check_ShouldReturnHealthStatus"

# Tests in package
./gradlew test --tests "com.tiagoramirez.template.health.*"

# Clean build with tests
./gradlew clean build
```

### View Coverage Report

```bash
# After running tests
open build/reports/jacoco/test/html/index.html
```

---

## Questions or Problems?

- **Open an issue**: For bugs, feature requests, or questions
- **Check documentation**:
  - [README.md](README.md) - Getting started
  - [ARCHITECTURE.md](ARCHITECTURE.md) - Hexagonal architecture guide
  - [docs/MONITORING.md](docs/MONITORING.md) - Prometheus/Grafana setup
  - [docs/VERIFICATION.md](docs/VERIFICATION.md) - Testing monitoring stack

---

Thank you for contributing to making this template better! 🎉
