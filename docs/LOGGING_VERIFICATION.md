# Log Aggregation Verification Guide

This guide provides step-by-step verification for the Loki log aggregation stack.

## Prerequisites

Ensure Docker Compose stack is running:
```bash
docker compose up -d --build
docker compose ps
```

All services should be **Up** and healthy.

---

## Step 1: Verify Loki is Running

Check Loki health endpoint:

```bash
curl http://localhost:3100/ready
```

**Expected output**: `ready`

If it returns an error:
- Check Loki logs: `docker logs loki`
- Verify Loki container is running: `docker compose ps loki`
- Check Loki configuration: `loki/loki-config.yml`

---

## Step 2: Verify Logs are Generated

Make requests to generate logs:

```bash
# Generate 10 test requests
for i in {1..10}; do
  curl -s http://localhost:8080/api/health
done
```

Check Docker container logs to verify logging is active:

```bash
# View recent logs (should show JSON format in pre-prod/prod)
docker logs java-app --tail 20
```

**Expected output** (pre-prod/prod profile):
- JSON formatted logs with fields: `level`, `message`, `http_status`, `http_method`, `http_path`, `trace_id`

**Expected output** (dev/test profile):
- Human-readable text format logs

**Troubleshooting**:
- If logs are not in JSON format, verify `SPRING_PROFILES_ACTIVE` is set to `pre-prod` or `prod`
- Check `docker compose ps` to see active profile
- Edit `.env` or `docker-compose.yaml` to set profile

---

## Step 3: Verify Promtail is Shipping Logs

Check Promtail is running and scraping logs:

```bash
# View Promtail logs
docker logs promtail --tail 50
```

**Look for**:
- `level=info msg="Starting Promtail"`
- `level=info msg="Successfully started promtail"`
- No error messages about connection to Loki

Check Promtail metrics (optional):

```bash
curl http://localhost:9080/metrics | grep promtail
```

**Troubleshooting**:
- If Promtail shows connection errors to Loki, check network: `docker network inspect java-template_monitoring`
- Verify Promtail config: `promtail/promtail-config.yml`
- Check Docker socket access: `ls -la /var/run/docker.sock` (permissions issue)

---

## Step 4: Verify Loki Has Logs

Query Loki API directly to confirm logs are stored:

```bash
# Query all logs for java-app container
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={container="java-app"}' \
  | jq .
```

**Expected output**: JSON response with `data.result` array containing log entries

**Alternative query (count logs)**:

```bash
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query=count_over_time({container="java-app"}[5m])' \
  | jq .
```

**Troubleshooting**:
- If `data.result` is empty, wait 30-60 seconds and retry
- Check Promtail is shipping: `docker logs promtail | grep "POST /loki/api/v1/push"`
- Verify container label: `docker inspect java-app | grep com.docker.compose.service`

---

## Step 5: Verify Grafana Can Query Logs

### Access Grafana Explore

1. Navigate to http://localhost:3000
2. Login with `admin` / `admin` (or your configured credentials)
3. Click **Explore** (compass icon in left sidebar)
4. Select **Loki** from datasource dropdown

### Run Basic Query

In the query builder:
1. Enter query: `{container="java-app"}`
2. Click **Run query** (or press Shift+Enter)

**Expected output**: List of log entries with timestamps and messages

**Troubleshooting**:
- If "No datasource found", check datasources: Configuration → Data Sources → Loki
- Test datasource connection: Click **Test** button (should show "Data source is working")
- If test fails, check `grafana/provisioning/datasources/datasource.yml`
- Restart Grafana: `docker compose restart grafana`

---

## Step 6: Test Log Filtering

Try various LogQL queries in Grafana Explore:

### Filter by log level (errors only)

```logql
{container="java-app"} | json | level="ERROR"
```

### Filter by HTTP status code (5xx errors)

```logql
{container="java-app"} | json | http_status >= 500
```

### Filter by endpoint

```logql
{container="java-app"} | json | http_path="/api/health"
```

### Filter by HTTP method

```logql
{container="java-app"} | json | http_method="GET"
```

### Slow requests (>100ms)

```logql
{container="java-app"} | json | http_duration_ms > 100
```

**Expected output**: Filtered logs matching the query criteria

**Troubleshooting**:
- If filters return empty results, ensure logs contain those fields (check raw logs)
- Verify JSON parsing in Promtail config: `promtail/promtail-config.yml`
- Labels must be extracted in Promtail `pipeline_stages.labels` section

---

## Step 7: Verify Dashboards Load and Display Data

### Access Application Logs Dashboard

1. In Grafana, click **Dashboards** (four squares icon)
2. Select **Application Logs** dashboard

**Expected panels**:
1. **Log Volume Over Time** - Graph showing log ingestion rate
2. **HTTP Status Code Distribution** - Pie chart of status codes
3. **Error Logs (5xx)** - Logs panel with server errors
4. **Client Errors (4xx)** - Logs panel with client errors
5. **Recent Application Logs** - All logs
6. **Response Time Distribution** - Heatmap of durations
7. **Top Endpoints by Request Count** - Table of most-hit endpoints
8. **Average Response Time by Endpoint** - Bar gauge

### Verify Each Panel

For each panel:
- Check if data is displayed (not "No data")
- Adjust time range if needed: Top right → "Last 15 minutes"
- Generate more requests if panels are empty: `curl http://localhost:8080/api/health`

**Troubleshooting**:
- If panels show "No data", wait 1-2 minutes for metrics to aggregate
- Check time range is appropriate (Last 15 minutes recommended)
- Verify queries are correct: Edit panel → View query
- Check Loki datasource is selected in panel settings

---

## Step 8: End-to-End Validation

Perform a complete end-to-end test:

```bash
# 1. Make requests with different patterns
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health

# 2. Wait 30 seconds for ingestion
sleep 30

# 3. Query Loki for recent logs
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={container="java-app"} | json | http_path="/api/health"' \
  | jq '.data.result | length'
```

**Expected output**: Number greater than 0 (count of matching log entries)

---

## Common Issues and Solutions

### Issue: "No datasource found" in Grafana

**Solution**:
```bash
# Check datasource provisioning
docker compose logs grafana | grep -i datasource

# Verify datasource file exists
cat grafana/provisioning/datasources/datasource.yml

# Restart Grafana
docker compose restart grafana
```

### Issue: Logs are in text format, not JSON

**Solution**:
```bash
# Check active Spring profile
docker compose exec app env | grep SPRING_PROFILES_ACTIVE

# Update docker-compose.yaml to set SPRING_PROFILES_ACTIVE=pre-prod
# Then restart
docker compose down
docker compose up -d --build
```

### Issue: Promtail cannot access Docker socket

**Solution** (Linux/macOS):
```bash
# Check Docker socket permissions
ls -la /var/run/docker.sock

# Fix permissions (may require sudo)
sudo chmod 666 /var/run/docker.sock

# Restart Promtail
docker compose restart promtail
```

### Issue: Loki shows connection refused

**Solution**:
```bash
# Check Loki is listening on correct port
docker compose exec loki netstat -tlnp | grep 3100

# Verify Loki is in correct network
docker network inspect java-template_monitoring

# Check Loki logs for errors
docker logs loki | tail -50
```

### Issue: High log volume, slow queries

**Solution**:
- Reduce retention period in `loki/loki-config.yml` (default: 7 days)
- Add more specific labels in Promtail to improve query performance
- Use LogQL filters to narrow down queries (e.g., time range, specific labels)

### Issue: Logs disappearing after 7 days

**Expected behavior**: Loki is configured with 7-day retention

**To extend retention**:
Edit `loki/loki-config.yml`:
```yaml
limits_config:
  retention_period: 336h  # 14 days
```

Then restart Loki:
```bash
docker compose restart loki
```

---

## Verification Checklist

Use this checklist to confirm all components are working:

- [ ] Loki health endpoint returns "ready"
- [ ] Application generates JSON logs (pre-prod/prod) or text logs (dev)
- [ ] Promtail is running without errors
- [ ] Loki API returns log entries when queried
- [ ] Grafana datasource "Loki" exists and tests successfully
- [ ] Grafana Explore can query logs with `{container="java-app"}`
- [ ] LogQL filters work (level, http_status, http_path, etc.)
- [ ] Application Logs dashboard loads with all 8 panels
- [ ] Panels display data (not "No data")
- [ ] End-to-end test returns log count > 0

---

## Performance Tips

### Optimize Log Query Performance

1. **Use specific time ranges**: Smaller time windows query faster
2. **Add label filters early**: `{container="java-app", http_status="200"}`
3. **Limit log lines**: Use `| line_format` or `| limit 100`
4. **Use aggregations**: `count_over_time()`, `rate()`, `sum()`

### Monitor Loki Resource Usage

```bash
# Check Loki memory usage
docker stats loki --no-stream

# Check disk usage for Loki data
docker exec loki du -sh /loki
```

**Recommended limits**:
- Memory: 512MB minimum, 2GB recommended
- Disk: 10GB for 7-day retention with moderate traffic

---

## Next Steps

After successful verification:

1. **Customize dashboards**: Add panels for your specific use cases
2. **Set up alerts**: Configure alerting rules for error rates, slow requests
3. **Add log sampling**: For high-traffic endpoints, consider sampling
4. **Integrate tracing**: Add distributed tracing (OpenTelemetry/Zipkin) for full observability
5. **Secure access**: Change default Grafana password, configure HTTPS

See [MONITORING.md](MONITORING.md) for advanced configuration options.
