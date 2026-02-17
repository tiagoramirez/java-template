# Monitoring & Observability Guide

## Architecture Overview

```
Application (Spring Boot)
    ↓ /api/actuator/prometheus
Prometheus (scrapes metrics every 10s)
    ↓ queries
Grafana (dashboards & visualization)
```

**Flow**: Spring Boot exposes metrics → Prometheus scrapes & stores → Grafana queries & displays

---

## Quick Start

```bash
# Start entire stack
docker compose up -d --build

# Wait 45s for services to initialize
docker compose ps

# Access services
curl http://localhost:8080/api/health
```

**Service URLs:**
- **Application**: http://localhost:8080/api/health
- **Metrics Endpoint**: http://localhost:8080/api/actuator/prometheus
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: Internal only (not exposed)

---

## Available Metrics

### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage (heap/non-heap)
- `jvm_threads_live` - Active thread count
- `jvm_gc_pause_seconds` - Garbage collection pause time
- `jvm_classes_loaded` - Loaded class count

### HTTP Metrics
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total request duration
- `http_server_requests_seconds_bucket` - Request duration histogram (p50, p95, p99)

### System Metrics
- `system_cpu_usage` - System CPU usage
- `process_cpu_usage` - Process CPU usage
- `process_uptime_seconds` - Application uptime

### Custom Metrics
All metrics tagged with:
- `application` - App name from `${spring.application.name}`
- `environment` - Environment from `${spring.profiles.active}`

---

## Useful PromQL Queries

### Top 5 slowest endpoints (p95)
```promql
topk(5, histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])))
```

### Error rate percentage
```promql
100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
      sum(rate(http_server_requests_seconds_count[5m]))
```

### Memory usage percentage
```promql
100 * (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"})
```

### Request rate (req/sec)
```promql
sum(rate(http_server_requests_seconds_count[1m]))
```

### Average response time
```promql
rate(http_server_requests_seconds_sum[5m]) /
rate(http_server_requests_seconds_count[5m])
```

---

## Adding Custom Metrics

### Counter Example
```java
@Component
public class OrderService {
    private final Counter orderCounter;

    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "online")
            .register(registry);
    }

    public void createOrder() {
        // Business logic
        orderCounter.increment();
    }
}
```

### Timer Example
```java
@Component
public class PaymentService {
    private final Timer paymentTimer;

    public PaymentService(MeterRegistry registry) {
        this.paymentTimer = Timer.builder("payment.processing.time")
            .description("Payment processing duration")
            .register(registry);
    }

    public void processPayment() {
        paymentTimer.record(() -> {
            // Payment logic
        });
    }
}
```

### @Timed Annotation
```java
@RestController
public class ApiController {

    @Timed(value = "api.custom.endpoint", description = "Custom endpoint timing")
    @GetMapping("/custom")
    public String customEndpoint() {
        return "response";
    }
}
```

---

## Troubleshooting

### Grafana shows "No Data"
1. Wait 2-3 minutes after startup
2. Adjust time range to "Last 15 minutes"
3. Verify datasource: Configuration → Data Sources → Prometheus → Test
4. Check Prometheus scraping: View logs with `docker logs prometheus`

### Prometheus target DOWN
```bash
# Check app health
docker compose ps app
curl http://localhost:8080/api/actuator/prometheus

# Check network connectivity
docker exec prometheus wget -O- http://app:8080/api/health
```

### Metrics not showing
1. Verify actuator endpoints exposed:
   ```bash
   curl http://localhost:8080/api/actuator
   ```
2. Check application.properties has metrics enabled
3. Restart services: `docker compose restart`

### Dashboard panels broken
Get Prometheus datasource UID:
```bash
curl -u admin:admin http://localhost:3000/api/datasources
```
Update dashboard JSON `datasource` fields with correct UID.

---

## Advanced Configuration

### Persistent Storage
Metrics persist across restarts via Docker volumes:
- `prometheus-data` - 30 days retention
- `grafana-data` - Dashboards & settings

### Data Retention
Modify retention in docker-compose.yaml:
```yaml
command:
  - '--storage.tsdb.retention.time=90d'  # Change from 30d
```

### Security
Default credentials in `.env`:
```
GF_ADMIN_USER=admin
GF_ADMIN_PASSWORD=admin  # Change for production
```

### Custom Dashboards
1. Create dashboard in Grafana UI
2. Export JSON: Dashboard Settings → JSON Model
3. Save to `grafana/provisioning/dashboards/custom-dashboard.json`
4. Restart Grafana: `docker compose restart grafana`