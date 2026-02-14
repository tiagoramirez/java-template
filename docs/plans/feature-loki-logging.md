# Implementation Plan: Grafana Loki Log Aggregation

## Context

The application currently has comprehensive metrics monitoring via Prometheus + Grafana but lacks centralized log aggregation. Users need to:
1. View application logs in Grafana dashboards alongside metrics
2. Search and filter logs by tags (HTTP status codes, endpoints, methods)
3. Troubleshoot issues by correlating logs with metrics

This enhancement adds Grafana Loki (log aggregation system) with structured JSON logging, enabling:
- Centralized log storage and querying via LogQL
- Real-time log search by tags (status codes, endpoints, duration)
- Log-metric correlation for complete observability
- Error detection and analysis (500 errors, exceptions)

The implementation follows hexagonal architecture principles, maintains 100% test coverage, and integrates seamlessly with the existing monitoring stack.

## Multi-Phase Implementation Plan

### Phase 1: Docker Infrastructure Setup

**Add Loki and Promtail services to monitoring stack**

#### 1.1 Update `docker-compose.yaml`

Add two new services:

**Loki service:**
- Image: `grafana/loki:2.9.10`
- Port: 3100 (internal + external for debugging)
- Config volume: `./loki/loki-config.yml:/etc/loki/local-config.yaml:ro`
- Data volume: `loki-data:/loki` (persistent storage)
- Health check: `wget http://localhost:3100/ready`
- Network: `monitoring`
- Restart: `unless-stopped`

**Promtail service:**
- Image: `grafana/promtail:2.9.10`
- Config volume: `./promtail/promtail-config.yml:/etc/promtail/config.yml:ro`
- Docker socket: `/var/run/docker.sock:/var/run/docker.sock` (read container logs)
- Container logs: `/var/lib/docker/containers:/var/lib/docker/containers:ro`
- Depends on: Loki health check passing
- Network: `monitoring`

Add new volume: `loki-data`

#### 1.2 Create `loki/loki-config.yml`

Configuration:
- Storage: Filesystem (chunks and index in `/loki`)
- Schema: TSDB v13 (modern time-series index)
- Retention: 7 days (168h)
- Ingestion limits: 10MB/s rate, 20MB burst
- Compaction: 10min interval with retention enforcement

#### 1.3 Create `promtail/promtail-config.yml`

Configuration:
- Scrape Docker containers filtered by label: `com.docker.compose.service=app`
- Parse JSON log format with pipeline stages
- Extract labels: `level`, `http_status`, `http_method`, `http_path`
- Extract fields: `trace_id`, `http_duration_ms`, `client_ip`, `user_agent`
- Ship to Loki at `http://loki:3100/loki/api/v1/push`

#### 1.4 Update `grafana/provisioning/datasources/datasource.yml`

Add Loki datasource:
- Name: `Loki`
- Type: `loki`
- URL: `http://loki:3100`
- Max lines: 1000
- Derived fields for trace correlation

**Verification:**
```bash
docker compose up -d --build
curl http://localhost:3100/ready  # Should return "ready"
```

---

### Phase 2: Spring Boot Structured JSON Logging

**Configure logback to output structured JSON logs**

#### 2.1 Update `build.gradle`

Add dependency:
```gradle
implementation 'net.logstash.logback:logstash-logback-encoder:8.0'
```

#### 2.2 Create `src/main/resources/logback-spring.xml`

Define two appenders:
- **CONSOLE_JSON**: Uses `LogstashEncoder` for structured JSON output
  - Custom fields: `application`, `environment` (from Spring properties)
  - MDC keys: `trace_id`, `http_method`, `http_path`, `http_status`, `http_duration_ms`, `client_ip`, `user_agent`
  - Shortened stack traces (max 30 depth, 2048 chars)

- **CONSOLE_TEXT**: Human-readable format for development

Profile-based routing:
- `dev`, `test` profiles → CONSOLE_TEXT
- `pre-prod`, `prod` profiles → CONSOLE_JSON

Log levels:
- `com.tiagoramirez.template`: DEBUG
- `org.springframework.web`: INFO

#### 2.3 Update `src/main/resources/application.properties`

Add:
```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
logging.level.com.tiagoramirez.template=DEBUG
logging.level.org.springframework.web=INFO
```

**Verification:**
```bash
./gradlew bootRun
# Observe text format logs in console

SPRING_PROFILES_ACTIVE=pre-prod ./gradlew bootRun
# Observe JSON format logs in console
```

---

### Phase 3: HTTP Request/Response Logging (Hexagonal Architecture)

**Create logging feature following ports & adapters pattern**

#### 3.1 Create Domain Model

**File:** `src/main/java/com/tiagoramirez/template/logging/domain/HttpLogEntry.java`

Java record with fields:
- `traceId` (String, required, unique per request)
- `method` (String, required, HTTP method)
- `path` (String, required, request URI)
- `query` (String, nullable, query parameters)
- `status` (int, HTTP status code)
- `durationMs` (long, request duration)
- `clientIp` (String, client IP or X-Forwarded-For)
- `userAgent` (String, User-Agent header)
- `requestHeaders` (Map<String, String>)
- `requestBody` (String, nullable)
- `responseBody` (String, nullable)
- `timestamp` (Instant, request time)

Validation in constructor:
- Non-null/blank checks for required fields
- Duration >= 0

Helper methods:
- `isError()`: status >= 400
- `isServerError()`: status >= 500

#### 3.2 Create Outbound Port

**File:** `src/main/java/com/tiagoramirez/template/logging/ports/out/LogPublisherPort.java`

Interface:
```java
public interface LogPublisherPort {
    void publish(HttpLogEntry logEntry);
}
```

#### 3.3 Create SLF4J Adapter

**File:** `src/main/java/com/tiagoramirez/template/logging/adapters/out/Slf4jLogPublisherAdapter.java`

Implementation:
- `@Component` annotation
- Implements `LogPublisherPort`
- Uses `Logger` for "HttpRequestLogger"
- Populates `MDC` with log entry fields
- Log level based on status:
  - >= 500: ERROR
  - >= 400: WARN
  - < 400: INFO
- Clears MDC in finally block

#### 3.4 Create HTTP Logging Filter

**File:** `src/main/java/com/tiagoramirez/template/logging/adapters/in/web/HttpLoggingFilter.java`

Implementation:
- Extends `OncePerRequestFilter`
- `@Component` + `@Order(Ordered.HIGHEST_PRECEDENCE)` annotations
- Constructor injection of `LogPublisherPort`
- Wraps request/response in `ContentCaching` wrappers
- Generates unique `traceId` (UUID)
- Measures duration with `System.currentTimeMillis()`
- Extracts client IP (X-Forwarded-For fallback to RemoteAddr)
- Creates `HttpLogEntry` and publishes via port
- Copies response body after logging
- Excludes actuator endpoints (`shouldNotFilter()`)

#### 3.5 Create Filter Configuration

**File:** `src/main/java/com/tiagoramirez/template/config/LoggingConfig.java`

Configuration:
- `@Configuration` class
- `FilterRegistrationBean<HttpLoggingFilter>` bean
- URL pattern: `/api/*`
- Order: 1

**Verification:**
```bash
./gradlew bootRun
curl http://localhost:8080/api/health
# Check console for JSON log with http_status=200
```

---

### Phase 4: Comprehensive Testing (100% Coverage)

**Create unit and integration tests**

#### 4.1 Domain Model Tests

**File:** `src/test/java/com/tiagoramirez/template/logging/domain/HttpLogEntryTest.java`

Test cases:
- Valid construction with all fields
- Null traceId throws IllegalArgumentException
- Blank traceId throws IllegalArgumentException
- Null method throws IllegalArgumentException
- Null path throws IllegalArgumentException
- Negative duration throws IllegalArgumentException
- Null timestamp throws IllegalArgumentException
- `isError()` returns true for status >= 400
- `isError()` returns false for status < 400
- `isServerError()` returns true for status >= 500
- `isServerError()` returns false for status < 500

#### 4.2 Adapter Tests

**File:** `src/test/java/com/tiagoramirez/template/logging/adapters/out/Slf4jLogPublisherAdapterTest.java`

Test cases:
- Publish with valid log entry doesn't throw exception
- Publish with 200 status logs at INFO level (verify via log capture)
- Publish with 400 status logs at WARN level
- Publish with 500 status logs at ERROR level
- MDC is cleared after publish

#### 4.3 Filter Tests

**File:** `src/test/java/com/tiagoramirez/template/logging/adapters/in/web/HttpLoggingFilterTest.java`

Test cases:
- Filter publishes log entry for GET /api/health
- Filter publishes log entry with correct method
- Filter publishes log entry with correct path
- Filter publishes log entry with correct status code
- Filter generates unique traceId
- Filter measures duration correctly (within tolerance)
- Filter extracts client IP from RemoteAddr
- Filter extracts client IP from X-Forwarded-For header
- Filter extracts User-Agent header
- `shouldNotFilter()` returns true for /api/actuator paths
- `shouldNotFilter()` returns false for /api/health
- Filter chain is called
- Response body is copied after logging

#### 4.4 Integration Tests

**File:** `src/test/java/com/tiagoramirez/template/logging/integration/HttpLoggingIntegrationTest.java`

Test cases:
- GET /api/health generates log entry (verify in log output)
- Log entry contains expected fields (parse from log output)
- Multiple requests generate multiple log entries with unique traceIds

**Run tests:**
```bash
./gradlew test
./gradlew jacocoTestReport
start build\reports\jacoco\test\html\index.html
# Verify 100% coverage for logging package
```

---

### Phase 5: Grafana Dashboards

**Create log visualization dashboards**

#### 5.1 Create Application Logs Dashboard

**File:** `grafana/provisioning/dashboards/logs-dashboard.json`

Panels:
1. **Log Volume Over Time** (Graph)
   - Query: `sum(count_over_time({container="java-app"}[1m]))`
   - Shows log ingestion rate

2. **HTTP Status Code Distribution** (Pie chart)
   - Query: `sum by (http_status) (count_over_time({container="java-app"} | json | http_status != "" [5m]))`
   - Visual breakdown of 2xx, 4xx, 5xx responses

3. **Error Logs (5xx)** (Logs panel)
   - Query: `{container="java-app"} | json | http_status >= 500`
   - Real-time server error logs

4. **Client Errors (4xx)** (Logs panel)
   - Query: `{container="java-app"} | json | http_status >= 400 and http_status < 500`
   - Client error logs

5. **Recent Application Logs** (Logs panel)
   - Query: `{container="java-app"}`
   - All logs with filtering capability

6. **Response Time Distribution** (Histogram)
   - Query: `{container="java-app"} | json | http_duration_ms != "" | unwrap http_duration_ms`
   - Response time heatmap

7. **Top Endpoints by Request Count** (Table)
   - Query: `topk(10, sum by (http_path) (count_over_time({container="java-app"} | json [5m])))`
   - Most-hit endpoints

8. **Average Response Time by Endpoint** (Bar gauge)
   - Query: `avg by (http_path) (avg_over_time({container="java-app"} | json | unwrap http_duration_ms [5m]))`
   - Performance per endpoint

#### 5.2 Update Existing Spring Boot Dashboard

**File:** `grafana/provisioning/dashboards/spring-boot-dashboard.json`

Add panel:
- **Recent Error Logs** (Logs panel at bottom)
- Query: `{container="java-app"} | json | level=~"ERROR|WARN"`
- Links metrics to relevant error logs

**Verification:**
```bash
docker compose restart grafana
# Navigate to http://localhost:3000
# Dashboards → Application Logs
# Verify all panels load and display data
```

---

### Phase 6: Documentation

#### 6.1 Update `docs/MONITORING.md`

Add section: **Log Aggregation with Loki**

Content:
- Architecture diagram (App → Promtail → Loki → Grafana)
- Available log fields and their meanings
- Useful LogQL query examples:
  - Filter by status: `{container="java-app"} | json | http_status >= 500`
  - Filter by endpoint: `{container="java-app"} | json | http_path="/api/health"`
  - Calculate p95 latency: `quantile_over_time(0.95, {container="java-app"} | json | unwrap http_duration_ms [5m])`
  - Count errors per minute: `sum(count_over_time({container="java-app"} | json | level="ERROR" [1m]))`
- Accessing logs in Grafana Explore
- Log retention policy (7 days)

#### 6.2 Update `CLAUDE.md`

Add section: **Logging**

Content:
- Structured JSON logging in pre-prod/prod
- Text logging in dev/test
- How to add logging to code (SLF4J examples)
- Using MDC for correlation IDs
- Viewing logs locally (docker logs) and in Grafana

#### 6.3 Update `README.md`

Add subsection under Monitoring: **Log Aggregation**

Content:
- Brief overview of Loki/Promtail
- How to query logs in Grafana
- Link to detailed docs/MONITORING.md

#### 6.4 Create `docs/LOGGING_VERIFICATION.md`

Step-by-step verification guide:
1. Verify Loki is running (`curl http://localhost:3100/ready`)
2. Verify logs are generated (make requests, check docker logs)
3. Verify Promtail is shipping logs (check Promtail metrics)
4. Verify Loki has logs (query Loki API)
5. Verify Grafana can query logs (Explore → Loki)
6. Test log filtering (various LogQL queries)
7. Verify dashboards load and display data
8. Troubleshooting section for common issues

---

### Phase 7: CI/CD Integration

#### 7.1 Update `.github/workflows/build.yml`

Add steps after test coverage:

```yaml
- name: Build and start Docker Compose stack
  run: docker compose up -d --build

- name: Wait for services to be healthy
  run: sleep 60

- name: Verify Loki health
  run: curl --fail http://localhost:3100/ready

- name: Generate test traffic
  run: |
    for i in {1..10}; do
      curl -s http://localhost:8080/api/health
    done

- name: Verify logs in Loki
  run: |
    sleep 10  # Allow time for log ingestion
    RESPONSE=$(curl -G -s "http://localhost:3100/loki/api/v1/query" \
      --data-urlencode 'query={container="java-app"}')
    echo $RESPONSE | jq -e '.data.result | length > 0'

- name: Tear down services
  run: docker compose down -v
```

---

## Implementation Sequence

1. **Phase 1** - Add Loki & Promtail to docker-compose, verify services start
2. **Phase 2** - Add logback-spring.xml, verify JSON logs in console (pre-prod profile)
3. **Phase 3** - Create logging domain, ports, adapters, filter; verify HTTP logs appear
4. **Phase 4** - Write all tests, verify 100% coverage with JaCoCo
5. **Phase 5** - Create Grafana dashboards, verify panels display log data
6. **Phase 6** - Update all documentation, follow verification guide
7. **Phase 7** - Update CI workflow, verify pipeline passes with log checks

---

## Critical Files

### New Files to Create:
- `loki/loki-config.yml` - Loki storage and retention configuration
- `promtail/promtail-config.yml` - Log scraping and parsing pipeline
- `src/main/resources/logback-spring.xml` - Structured JSON logging config
- `src/main/java/com/tiagoramirez/template/logging/domain/HttpLogEntry.java` - Domain model
- `src/main/java/com/tiagoramirez/template/logging/ports/out/LogPublisherPort.java` - Outbound port
- `src/main/java/com/tiagoramirez/template/logging/adapters/out/Slf4jLogPublisherAdapter.java` - SLF4J adapter
- `src/main/java/com/tiagoramirez/template/logging/adapters/in/web/HttpLoggingFilter.java` - HTTP filter
- `src/main/java/com/tiagoramirez/template/config/LoggingConfig.java` - Filter registration
- `grafana/provisioning/dashboards/logs-dashboard.json` - Logs dashboard
- `docs/LOGGING_VERIFICATION.md` - Verification guide
- All test files (11 test classes covering domain, adapters, filter, integration)

### Files to Modify:
- `docker-compose.yaml` - Add Loki, Promtail services and volume
- `build.gradle` - Add logstash-logback-encoder dependency
- `src/main/resources/application.properties` - Add logging configuration
- `grafana/provisioning/datasources/datasource.yml` - Add Loki datasource
- `grafana/provisioning/dashboards/spring-boot-dashboard.json` - Add log correlation panel
- `docs/MONITORING.md` - Add log aggregation section
- `CLAUDE.md` - Add logging guidance
- `README.md` - Add log aggregation overview
- `.github/workflows/build.yml` - Add log verification steps

---

## Verification Steps

### End-to-End Verification:

1. **Start services:** `docker compose up -d --build`
2. **Check all services healthy:** `docker compose ps`
3. **Verify Loki:** `curl http://localhost:3100/ready`
4. **Generate logs:** `curl http://localhost:8080/api/health` (10x)
5. **Query Loki API:** `curl -G -s "http://localhost:3100/loki/api/v1/query" --data-urlencode 'query={container="java-app"}' | jq .`
6. **Grafana Explore:** Navigate to http://localhost:3000 → Explore → Loki → Query: `{container="java-app"}`
7. **Test filters:** `{container="java-app"} | json | http_status = "200"`
8. **Check dashboard:** Dashboards → Application Logs → Verify all panels display data
9. **Run tests:** `./gradlew test jacocoTestReport` → Verify 100% coverage

---

## Unresolved Questions

1. Request/response body logging needed? (currently excluded - security/size)
2. Log retention period OK? (default: 7 days)
3. Alerting rules on logs? (e.g., alert on 5xx spike)
4. Log sampling for high-traffic endpoints?
5. Sensitive data masking requirements? (passwords, tokens, PII)
6. Distributed tracing integration later? (Spring Cloud Sleuth/OpenTelemetry)
7. Log level per package config needed beyond defaults?
8. Alternative: Loki Docker Driver instead of Promtail? (simpler but less flexible)

---

## Recommended Commit Message

```
feat: add Grafana Loki log aggregation with structured JSON logging

- Add Loki and Promtail services to docker-compose monitoring stack
- Implement structured JSON logging with Logstash encoder for pre-prod/prod
- Create HTTP request/response logging filter following hexagonal architecture
- Add comprehensive log aggregation dashboards in Grafana with status code filtering
- Enable log search by tags: status codes, endpoints, methods, duration
- Add LogPublisherPort (outbound port) and Slf4jLogPublisherAdapter
- Update monitoring documentation with LogQL query examples
- Add unit and integration tests achieving 100% coverage
- Configure 7-day log retention with automatic compaction
- Add CI/CD verification steps for log ingestion

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

---

## Branch Name

`feature/loki-logging`
