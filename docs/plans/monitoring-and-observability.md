# Prometheus & Grafana Monitoring Setup - Implementation Plan

## Overview

Fix the Spring Boot 4 migration issue with `MeterRegistryCustomizer`, complete the Prometheus/Grafana setup with auto-provisioning, and provide usage documentation.

## Root Cause Analysis

**Current Issue**: `MeterRegistryCustomizer cannot be resolved to a type` in `AppConfig.java:33`

**Cause**: Spring Boot 4 modularization moved the class:
- Old: `org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer`
- New: `org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer`

**Secondary Issue**: `micrometer-registry-prometheus` is `runtimeOnly` but used at compile-time in `AppConfig.java`

## Implementation Plan

### ✅ Phase 1: Fix Compilation Errors (Priority 1)

#### 1.1 Fix MeterRegistryCustomizer Import
**File**: `src/main/java/com/tiagoramirez/template/config/AppConfig.java`

**Change line 4 from:**
```java
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
```

**To:**
```java
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
```

#### 1.2 Update Gradle Dependency Scope
**File**: `build.gradle`

**Change line 40 from:**
```gradle
runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
```

**To:**
```gradle
implementation 'io.micrometer:micrometer-registry-prometheus'
```

**Rationale**: `MeterRegistryCustomizer<MeterRegistry>` is used at compile-time, so dependency must be available during compilation.

**Verification**: Run `./gradlew clean build` - should compile successfully.

---

### ✅ Phase 2: Grafana Auto-Provisioning (Priority 2)

#### 2.1 Create Grafana Datasource Provisioning
**New File**: `grafana/provisioning/datasources/datasource.yml`

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      httpMethod: POST
      timeInterval: 15s
```

This auto-configures Prometheus as the default datasource using Docker service name for networking.

#### 2.2 Create Dashboard Provisioning Configuration
**New File**: `grafana/provisioning/dashboards/dashboard.yml`

```yaml
apiVersion: 1

providers:
  - name: 'Spring Boot'
    orgId: 1
    folder: 'Spring Boot Monitoring'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
```

#### 2.3 Add Pre-Built Dashboard
**New File**: `grafana/provisioning/dashboards/spring-boot-dashboard.json`

**Action**: Download Grafana Dashboard ID 4701 (JVM Micrometer) from https://grafana.com/grafana/dashboards/4701-jvm-micrometer/

**Key panels include**:
- JVM Memory (heap/non-heap)
- CPU Usage
- Thread Count
- HTTP Request Rate & Duration (p50, p95, p99)
- Garbage Collection Activity

**Customization needed**: Update datasource references to use `"datasource": "Prometheus"`

---

### ✅ Phase 3: Docker Compose Enhancement (Priority 2)

#### 3.1 Update Docker Compose Configuration
**File**: `docker-compose.yaml`

**Key additions**:

1. **Named Network** for explicit service communication:
```yaml
networks:
  monitoring:
    driver: bridge
```

2. **Named Volumes** for persistence:
```yaml
volumes:
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
```

3. **Health Checks** for proper startup ordering:
```yaml
services:
  app:
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 40s
```

4. **Prometheus enhancements**:
```yaml
  prometheus:
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    depends_on:
      app:
        condition: service_healthy
```

5. **Grafana provisioning mount**:
```yaml
  grafana:
    environment:
      - GF_SECURITY_ADMIN_USER=${GF_ADMIN_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${GF_ADMIN_PASSWORD:-admin}
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning:ro
```

**Benefits**:
- Metrics/dashboards persist across restarts
- Grafana auto-configures on startup
- Proper dependency ordering ensures Prometheus doesn't scrape unhealthy app

---

### ✅ Phase 4: Enhanced Metrics Configuration (Priority 3)

#### 4.1 Update Application Properties
**File**: `src/main/resources/application.properties`

**Add comprehensive metrics configuration:**

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
management.endpoint.prometheus.access=unrestricted
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true

# Metrics Configuration
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
management.metrics.enable.http=true
management.metrics.distribution.percentiles-histogram.http.server.requests=true
management.metrics.tags.application=${spring.application.name}
management.metrics.tags.environment=${spring.profiles.active}

# Prometheus Configuration
management.prometheus.metrics.export.enabled=true
management.prometheus.metrics.export.step=10s
```

**Key improvements**:
- Histogram enabled for HTTP request percentiles (p50, p95, p99)
- Kubernetes-style liveness/readiness probes
- Automatic tagging with application name and environment
- Detailed health information

#### 4.2 Enhance Prometheus Configuration
**File**: `prometheus/prometheus.yml`

**Update with enhanced scraping:**

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'spring-boot-monitor'
    environment: '${SPRING_PROFILES_ACTIVE:-dev}'

scrape_configs:
  - job_name: 'spring-app'
    metrics_path: '/api/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
        labels:
          application: '${APP_NAME:-template}'
          service: 'spring-boot-app'
    scrape_interval: 10s
    scrape_timeout: 5s

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
        labels:
          service: 'prometheus'
```

**Benefits**:
- Self-monitoring of Prometheus
- Environment identification via labels
- More frequent scraping for better granularity

#### 4.3 Update Environment Variables
**File**: `.env`

**Add:**
```
# Grafana Admin Credentials
GF_ADMIN_USER=admin
GF_ADMIN_PASSWORD=changeme_in_production
```

---

### Phase 5: Documentation (Priority 3)

#### 5.1 Create Monitoring Guide
**New File**: `docs/MONITORING.md`

**Sections**:
1. **Architecture Overview** - Diagram showing metric flow
2. **Quick Start** - How to start the stack
3. **Access Information** - URLs for all services
4. **Available Metrics** - List of JVM, HTTP, system, custom metrics
5. **Useful Queries** - Example PromQL queries (top slow endpoints, error rate, memory usage)
6. **Troubleshooting** - Common issues and solutions
7. **Adding Custom Metrics** - Code examples for Counter, Timer, @Timed annotation
8. **Advanced Configuration** - Tracing, alerting, custom dashboards

**Key content examples**:

**Quick Start:**
```bash
docker compose up -d --build

# Access services:
# App:        http://localhost:8080/api/health
# Prometheus: http://localhost:9090
# Grafana:    http://localhost:3000 (admin/admin)
```

**Example PromQL Queries:**
```promql
# Top 5 slowest endpoints (p95)
topk(5, histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])))

# Error rate percentage
100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
      sum(rate(http_server_requests_seconds_count[5m]))
```

**Adding Custom Metrics:**
```java
@Component
public class MyService {
    private final Counter myCounter;

    public MyService(MeterRegistry registry) {
        this.myCounter = Counter.builder("my.custom.counter")
            .description("My custom counter")
            .register(registry);
    }

    public void doSomething() {
        myCounter.increment();
    }
}
```

#### 5.2 Create Verification Checklist
**New File**: `docs/VERIFICATION.md`

**Sections**:
1. Pre-flight checks (Java, Docker, ports)
2. Build verification (`./gradlew clean build`)
3. Startup verification (`docker compose ps`)
4. Application health check
5. Prometheus verification (UI, targets, test query)
6. Grafana verification (login, datasource, dashboards)
7. Load testing (generate traffic, verify metrics)
8. Persistence verification (restart and check data retained)
9. Common issues troubleshooting
10. Success criteria checklist

#### 5.3 Update Main README
**File**: `README.md`

**Changes**:
1. Update line 77: Remove "(setup required with prometheus metrics)"
2. Add feature bullets: "Automated Grafana provisioning" and "Persistent metric storage"
3. Update troubleshooting section (lines 118-119): "Datasource is auto-provisioned on startup"
4. Add link to `docs/MONITORING.md` in Quick Start section

---

## Critical Files Summary

### Files to Modify (5):
1. `src/main/java/com/tiagoramirez/template/config/AppConfig.java` - Fix import (line 4)
2. `build.gradle` - Change dependency scope (line 40)
3. `docker-compose.yaml` - Add networks, volumes, provisioning, health checks
4. `prometheus/prometheus.yml` - Enhanced scrape configuration
5. `src/main/resources/application.properties` - Comprehensive metrics config

### Files to Create (8):
1. `grafana/provisioning/datasources/datasource.yml` - Datasource auto-config
2. `grafana/provisioning/dashboards/dashboard.yml` - Dashboard provider config
3. `grafana/provisioning/dashboards/spring-boot-dashboard.json` - Pre-built dashboard
4. `docs/MONITORING.md` - Complete usage guide
5. `docs/VERIFICATION.md` - Testing checklist
6. `.env` - Add Grafana credentials (update existing)
7. `README.md` - Update with provisioning info (update existing)
8. `scripts/test-monitoring.sh` - Optional E2E test script

---

## Implementation Sequence

### Step 1: Fix Compilation (5 minutes)
1. Update `AppConfig.java` import statement
2. Update `build.gradle` dependency scope
3. Run `./gradlew clean build`
4. Verify: BUILD SUCCESSFUL

### Step 2: Setup Grafana Provisioning (20 minutes)
1. Create directory: `grafana/provisioning/datasources/`
2. Create `datasource.yml`
3. Create directory: `grafana/provisioning/dashboards/`
4. Create `dashboard.yml`
5. Download Dashboard ID 4701 JSON, save as `spring-boot-dashboard.json`

### Step 3: Update Docker Configuration (15 minutes)
1. Update `docker-compose.yaml` with networks, volumes, provisioning
2. Update `prometheus/prometheus.yml` with enhanced config
3. Update `.env` with Grafana credentials

### Step 4: Enhance Application Config (10 minutes)
1. Update `application.properties` with comprehensive metrics config
2. Run `./gradlew clean build` to verify

### Step 5: Test the Stack (10 minutes)
1. Run `docker compose up -d --build`
2. Wait 45 seconds for services to initialize
3. Verify all services: `docker compose ps`
4. Check app health: `curl http://localhost:8080/api/health`
5. Check Prometheus targets: http://localhost:9090/targets (should show spring-app UP)
6. Login to Grafana: http://localhost:3000 (admin/admin)
7. Verify datasource auto-configured: Configuration → Data Sources
8. Verify dashboard auto-loaded: Dashboards → Browse → "Spring Boot Monitoring"
9. Open dashboard, verify panels show data

### Step 6: Create Documentation (30 minutes)
1. Create `docs/MONITORING.md` with usage guide
2. Create `docs/VERIFICATION.md` with test checklist
3. Update `README.md` with provisioning info

### Step 7: Optional Enhancements (20 minutes)
1. Create `scripts/test-monitoring.sh` for automated E2E testing
2. Make executable: `chmod +x scripts/test-monitoring.sh`
3. Run test script to validate

**Total Time**: ~110 minutes (1.5-2 hours)

---

## Verification Plan

### End-to-End Test Flow:

1. **Build**: `./gradlew clean build` → BUILD SUCCESSFUL
2. **Start**: `docker compose up -d --build` → All services UP
3. **App Health**: `curl http://localhost:8080/api/health` → HTTP 200
4. **Metrics**: `curl http://localhost:8080/api/actuator/prometheus` → Returns metrics
5. **Prometheus**: http://localhost:9090/targets → spring-app target UP
6. **Prometheus Query**: Query `jvm_memory_used_bytes` → Shows data
7. **Grafana Login**: http://localhost:3000 → Login successful (admin/admin)
8. **Grafana Datasource**: Configuration → Data Sources → Prometheus exists and works
9. **Grafana Dashboard**: Dashboards → Spring Boot Monitoring → Dashboard loads
10. **Dashboard Data**: All panels show metrics (wait 1-2 min if needed)
11. **Generate Load**: Run 50 requests to `/api/health`
12. **Verify Metrics**: Dashboard shows increased request count
13. **Persistence**: `docker compose down && docker compose up -d` → Data retained

### Success Criteria:
- ✅ No compilation errors
- ✅ All Docker services healthy
- ✅ Prometheus scrapes app every 10 seconds
- ✅ Grafana datasource auto-configured (no manual setup)
- ✅ Dashboard auto-loaded with working panels
- ✅ Metrics persist after restart
- ✅ Documentation complete

---

## Troubleshooting Guide

### Issue: MeterRegistryCustomizer still not resolved
**Solution**: Clear Gradle cache and rebuild
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Issue: Prometheus target shows as DOWN
**Check**:
1. App container healthy: `docker compose ps app`
2. Metrics endpoint: `curl http://localhost:8080/api/actuator/prometheus`
3. Network connectivity: `docker exec prometheus wget -O- http://app:8080/api/health`

### Issue: Grafana shows "No Data"
**Solutions**:
1. Wait 2-3 minutes after startup
2. Adjust time range to "Last 15 minutes"
3. Verify datasource: Configuration → Data Sources → Prometheus → Test

### Issue: Grafana provisioning not working
**Check**:
1. Verify directory structure exists
2. Check Grafana logs: `docker logs grafana | grep provisioning`
3. Verify volume mount: `docker inspect grafana | grep Mounts`

### Issue: Dashboard panels broken
**Solution**: Dashboard JSON may need datasource UID updated. Get Prometheus UID:
```bash
curl -u admin:admin http://localhost:3000/api/datasources
```
Update dashboard JSON `datasource` fields with correct UID.

---

## Additional Notes

### Data Retention
- **Prometheus**: 30 days (configurable via `--storage.tsdb.retention.time`)
- **Grafana**: Persistent across restarts (stored in `grafana-data` volume)

### Security Considerations
- Default Grafana credentials should be changed in `.env` for production
- Add `.env` to `.gitignore` (should already be present)
- Consider adding authentication to Prometheus for production

### Performance Impact
- Metrics collection adds ~5-10MB memory overhead
- Prometheus scraping every 10s is minimal network impact
- Dashboard queries should be optimized for production (avoid high-cardinality queries)

### Future Enhancements (Out of Scope)
- Alerting with Prometheus Alertmanager
- Distributed tracing with OpenTelemetry
- Log aggregation with Loki
- Multi-environment dashboard separation
- OAuth authentication for Grafana

---

## References

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [Grafana Provisioning Guide](https://grafana.com/docs/grafana/latest/administration/provisioning/)
- [Prometheus Docker Setup](https://prometheus.io/docs/prometheus/latest/installation/)
- [JVM Micrometer Dashboard](https://grafana.com/grafana/dashboards/4701-jvm-micrometer/)
