# Architecture

## Overview

This project implements **Hexagonal Architecture** (also known as **Ports and Adapters** pattern), an architectural pattern that promotes separation of concerns and creates a clear boundary between business logic and external dependencies.

### Key Benefits

- **Testability**: Business logic can be tested in isolation without external dependencies
- **Flexibility**: Easy to swap implementations (e.g., change database, switch APIs)
- **Domain Isolation**: Core business logic is independent of frameworks and infrastructure
- **Maintainability**: Clear separation makes code easier to understand and modify

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      External World                         │
│   (HTTP Requests, Databases, External APIs, Time, etc.)     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    INPUT ADAPTERS                           │
│              (adapters/in/web/)                             │
│   • REST Controllers      • DTOs      • Request Mapping     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     INPUT PORTS                             │
│                   (ports/in/)                               │
│              Define Use Case Interfaces                     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   APPLICATION LAYER                         │
│                   (application/)                            │
│         Services implementing business use cases            │
└──────────┬──────────────────────────────────┬───────────────┘
           │                                  │
           │                                  ▼
           │                      ┌────────────────────────┐
           │                      │    OUTPUT PORTS        │
           │                      │     (ports/out/)       │
           │                      │  Define dependency     │
           │                      │     interfaces         │
           │                      └──────────┬─────────────┘
           │                                 │
           ▼                                 ▼
┌──────────────────────┐       ┌────────────────────────────┐
│   DOMAIN LAYER       │       │   OUTPUT ADAPTERS          │
│    (domain/)         │       │    (adapters/out/)         │
│  • Entities          │       │  Implement external        │
│  • Value Objects     │       │  dependencies              │
│  • Business Rules    │       └────────────────────────────┘
└──────────────────────┘
```

**Dependency Rule**: Dependencies point INWARD. Domain depends on nothing. Application depends on ports. Adapters depend on ports.

---

## Layer Responsibilities

### 1. Domain Layer (`domain/`)

The **heart** of the application containing pure business logic.

**Characteristics**:
- No framework dependencies (no Spring annotations)
- No infrastructure concerns
- Contains entities, value objects, and domain rules
- Should be framework-agnostic and portable

**Example**: `HealthStatus.java`
```java
public record HealthStatus(String message, Instant timestamp) {
    public static HealthStatus ok(Instant timestamp) {
        return new HealthStatus("I'm alive!", timestamp);
    }
}
```

---

### 2. Application Layer (`application/`)

Contains **use case implementations** (business orchestration).

**Characteristics**:
- Implements input ports (use case interfaces)
- Depends on output ports (not implementations)
- Orchestrates domain objects
- Framework annotations allowed (@Component, @Service)

**Example**: `HealthService.java`
```java
@Component
public class HealthService implements HealthPort {
    private final TimeProviderPort timeProvider;

    public HealthService(TimeProviderPort timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public HealthStatus check() {
        return HealthStatus.ok(timeProvider.getCurrentTime());
    }
}
```

**Key Points**:
- Implements `HealthPort` (input port)
- Depends on `TimeProviderPort` (output port interface, not implementation)
- Uses constructor injection for testability

---

### 3. Ports

Ports are **interfaces** that define contracts between layers.

#### Input Ports (`ports/in/`)

Define **use case interfaces** (what the application can do).

**Example**: `HealthPort.java`
```java
public interface HealthPort {
    HealthStatus check();
}
```

Consumed by: Input adapters (controllers)
Implemented by: Application services

#### Output Ports (`ports/out/`)

Define **dependency interfaces** (what the application needs from external world).

**Example**: `TimeProviderPort.java`
```java
public interface TimeProviderPort {
    Instant getCurrentTime();
}
```

Consumed by: Application services
Implemented by: Output adapters

---

### 4. Adapters

Adapters are **implementations** that connect the core to the external world.

#### Input Adapters (`adapters/in/web/`)

Translate external requests into use case calls.

**Contains**:
- REST Controllers
- Request/Response DTOs
- HTTP-specific logic

**Example**: `HealthController.java`
```java
@RestController
@RequestMapping("/health")
public class HealthController {
    private final HealthPort healthPort;

    public HealthController(HealthPort healthPort) {
        this.healthPort = healthPort;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthResponseDto> check() {
        HealthStatus healthStatus = healthPort.check();
        return ResponseEntity.ok(
            new HealthResponseDto(
                healthStatus.message(),
                healthStatus.timestamp()
            )
        );
    }
}
```

**Key Points**:
- Depends on `HealthPort` (interface), not `HealthService` (implementation)
- Handles HTTP concerns (mapping, status codes, response format)
- Converts domain objects to DTOs

#### Output Adapters (`adapters/out/`)

Implement external dependencies (databases, APIs, system resources).

**Example**: `TimeProviderAdapter.java`
```java
@Component
public class TimeProviderAdapter implements TimeProviderPort {
    @Override
    public Instant getCurrentTime() {
        return Instant.now();
    }
}
```

---

## Package Structure by Feature

Each feature follows this structure:

```
com.tiagoramirez.template.<feature>/
├── adapters/
│   ├── in/
│   │   └── web/              # REST controllers, DTOs
│   └── out/                  # External system adapters (DB, APIs, etc.)
├── application/              # Services (use case implementations)
├── domain/                   # Entities, value objects, business rules
└── ports/
    ├── in/                   # Input ports (use case interfaces)
    └── out/                  # Output ports (dependency interfaces)
```

---

## Dependency Flow

### The Dependency Inversion Principle

```
Controller (adapter) ──depends on──> HealthPort (interface)
                                         ▲
                                         │ implements
                                         │
                                    HealthService (application)
                                         │
                                         │ depends on
                                         ▼
                                  TimeProviderPort (interface)
                                         ▲
                                         │ implements
                                         │
                                  TimeProviderAdapter (adapter)
```

**Critical Rule**: Dependencies point INWARD toward the domain.

- **Domain** depends on: Nothing
- **Application** depends on: Domain, Ports (interfaces)
- **Adapters** depend on: Ports (interfaces)
- **Ports** depend on: Domain (for types)

Spring's dependency injection wires adapters to ports at runtime.

---

## Example Walkthrough: Health Feature

### Request Flow

```
1. HTTP GET /api/health
   │
   ▼
2. HealthController (input adapter)
   │  - Receives HTTP request
   │  - Calls healthPort.check()
   │
   ▼
3. HealthPort (input port interface)
   │  - Contract: HealthStatus check()
   │
   ▼
4. HealthService (application layer)
   │  - Implements HealthPort
   │  - Calls timeProvider.getCurrentTime()
   │
   ▼
5. TimeProviderPort (output port interface)
   │  - Contract: Instant getCurrentTime()
   │
   ▼
6. TimeProviderAdapter (output adapter)
   │  - Implements TimeProviderPort
   │  - Returns Instant.now()
   │
   ◄──┘
7. Flow returns through layers
   │
   ▼
8. HTTP Response with JSON
```

### Why Each Layer Exists

**HealthController** (Input Adapter):
- Handles HTTP concerns (routes, status codes, JSON)
- Isolates REST API changes from business logic
- Could be replaced with GraphQL adapter without changing business logic

**HealthPort** (Input Port):
- Defines use case contract
- Enables testing controllers without real service
- Documents what the application can do

**HealthService** (Application):
- Implements business logic
- Testable in isolation by mocking `TimeProviderPort`
- Framework-independent (could work outside Spring)

**TimeProviderPort** (Output Port):
- Abstracts system time dependency
- Enables testing with fixed time (no flaky tests)
- Could be replaced with NTP time source without changing service

**TimeProviderAdapter** (Output Adapter):
- Provides real implementation of time source
- Changes here don't affect business logic
- Easy to swap implementations

---

## Adding New Features

Follow this step-by-step guide to add a new feature using hexagonal architecture:

### Step 1: Create Feature Package

```
com.tiagoramirez.template.<feature-name>/
```

### Step 2: Define Domain Entities

Create entities and value objects in `domain/`:
- Pure business logic
- No framework dependencies
- Use records for immutability where appropriate

```java
// domain/User.java
public record User(String id, String name, String email) {
    // Business rules here
}
```

### Step 3: Define Input Ports

Create use case interfaces in `ports/in/`:

```java
// ports/in/CreateUserPort.java
public interface CreateUserPort {
    User create(String name, String email);
}
```

### Step 4: Implement Application Service

Create service in `application/` that implements input port:

```java
// application/UserService.java
@Component
public class UserService implements CreateUserPort {
    private final UserRepositoryPort repository;

    public UserService(UserRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public User create(String name, String email) {
        User user = new User(UUID.randomUUID().toString(), name, email);
        return repository.save(user);
    }
}
```

### Step 5: Define Output Ports (if needed)

If service needs external dependencies, create output port in `ports/out/`:

```java
// ports/out/UserRepositoryPort.java
public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(String id);
}
```

### Step 6: Implement Output Adapters

Create adapter in `adapters/out/`:

```java
// adapters/out/InMemoryUserRepository.java
@Component
public class InMemoryUserRepository implements UserRepositoryPort {
    private final Map<String, User> storage = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        storage.put(user.id(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }
}
```

### Step 7: Create Input Adapter (Controller)

Create controller in `adapters/in/web/`:

```java
// adapters/in/web/UserController.java
@RestController
@RequestMapping("/users")
public class UserController {
    private final CreateUserPort createUserPort;

    public UserController(CreateUserPort createUserPort) {
        this.createUserPort = createUserPort;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> create(@RequestBody CreateUserRequestDto request) {
        User user = createUserPort.create(request.name(), request.email());
        return ResponseEntity.ok(new UserResponseDto(user.id(), user.name(), user.email()));
    }
}
```

### Step 8: Write Tests

**Unit Test Service** (mock output ports):
```java
class UserServiceTest {
    @Mock
    private UserRepositoryPort repository;

    @InjectMocks
    private UserService service;

    @Test
    void create_ShouldSaveUser() {
        // Test service in isolation
    }
}
```

**Integration Test** (full stack):
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserIntegrationTest {
    @Test
    void createUser_ShouldReturn200() {
        // Test full HTTP request/response
    }
}
```

---

## Testing Strategy

### Unit Tests

Test each layer in isolation:

**Domain Tests**:
- Test business rules and entity behavior
- No mocks needed (pure functions)
- Example: `HealthStatusTest.java`

**Service Tests**:
- Mock output ports
- Test business logic orchestration
- Example: `HealthServiceTest.java`

**Adapter Tests**:
- Mock ports they depend on
- Test adapter-specific logic
- Example: `HealthControllerTest.java` (mock `HealthPort`)

### Integration Tests

Test full request/response cycle:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthTest {
    // Test with RestAssured
    // Full Spring context loaded
    // Real HTTP requests
}
```

See `src/test/java/com/tiagoramirez/template/health/` for complete examples.

---

## Configuration

**AppConfig.java** (`config/`):
- Global Spring beans
- ObjectMapper configuration (snake_case JSON)
- OpenAPI configuration
- Prometheus common tags

Spring Boot's dependency injection automatically wires:
- Controllers → Input Ports → Services
- Services → Output Ports → Adapters

No manual wiring needed beyond `@Component` annotations.

---

## References

- Hexagonal Architecture
- Ports & Adapters Pattern
- Clean Architecture
- Domain-Driven Design
