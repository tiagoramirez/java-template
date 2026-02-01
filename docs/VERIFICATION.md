# Monitoring Stack Verification Checklist

## Pre-flight Checks

- [ ] Java 25 installed: `java -version`
- [ ] Docker running: `docker ps`
- [ ] Ports available: 8080 (app), 3000 (Grafana)
- [ ] Git repo clean: `git status`

---

## 1. Build Verification

```bash
./gradlew clean build
```

**Expected**: `BUILD SUCCESSFUL`

**If fails**:
- Check Java version compatibility
- Run `./gradlew build --refresh-dependencies`
- Check for compilation errors in AppConfig.java

---

## 2. Start Services

```bash
docker compose up -d --build
```

**Expected**: 3 containers created (app, prometheus, grafana)

**Verify**:
```bash
docker compose ps
```

All services should show `Up` status after ~45s.

---

## 3. Application Health Check

```bash
curl http://localhost:8080/api/health
```

**Expected**:
```json
{"message":"I'm alive!"}
```

**If fails**:
- Check logs: `docker logs java-app`
- Verify port 8080 not in use: `netstat -an | grep 8080`
- Check healthcheck: `docker inspect java-app | grep Health`

---

## 4. Metrics Endpoint

```bash
curl http://localhost:8080/api/actuator/prometheus
```

**Expected**: Prometheus metrics output starting with:
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{...}
```

**If fails**:
- Check application.properties has actuator endpoints exposed
- Verify `management.endpoints.web.exposure.include=prometheus`

---

## 5. Prometheus Verification

### Check Targets
Open: http://localhost:9090/targets (accessible only from host, not exposed externally)

**Expected**:
- `spring-app` target shows `UP` state
- Last scrape < 15s ago

### Test Query
Navigate to: http://localhost:9090/graph

Query: `jvm_memory_used_bytes`

**Expected**: Graph shows memory usage data

**If target DOWN**:
- Check app container health: `docker compose ps app`
- Verify network: `docker network inspect java-template_monitoring`
- Check prometheus logs: `docker logs prometheus`

---

## 6. Grafana Verification

### Login
1. Open: http://localhost:3000
2. Username: `admin`
3. Password: Check `.env` file (default: `admin`)

**Expected**: Successful login

### Verify Datasource
1. Navigate: Configuration → Data Sources
2. Click "Prometheus"
3. Click "Save & Test"

**Expected**: "Data source is working" message

**If fails**:
- Check datasource URL: `http://prometheus:9090`
- Verify prometheus container running: `docker compose ps prometheus`
- Check grafana logs: `docker logs grafana | grep provisioning`

### Verify Dashboard
1. Navigate: Dashboards → Browse
2. Look for folder: "Spring Boot Monitoring"
3. Open dashboard

**Expected**: Dashboard loads with multiple panels

**If missing**:
- Check provisioning files exist: `ls grafana/provisioning/dashboards/`
- Verify volume mount: `docker inspect grafana | grep provisioning`
- Restart grafana: `docker compose restart grafana`

---

## 7. Dashboard Data Verification

Open the Spring Boot dashboard in Grafana.

**Check panels show data** (wait 1-2 min if needed):
- [ ] JVM Memory panel shows memory usage
- [ ] CPU Usage panel shows CPU metrics
- [ ] HTTP Requests panel shows request count
- [ ] Thread Count panel shows thread metrics

**If "No Data"**:
- Adjust time range to "Last 15 minutes"
- Wait 2-3 minutes for metrics to accumulate
- Verify Prometheus is scraping: Check Prometheus UI targets

---

## 8. Load Testing

Generate traffic:
```bash
for i in {1..50}; do curl -s http://localhost:8080/api/health > /dev/null; done
```

**Expected**:
- Dashboard shows increased request count
- Response time metrics update
- No errors in application logs

---

## 9. Persistence Verification

```bash
# Stop services
docker compose down

# Start again (without --build)
docker compose up -d

# Wait 30s
sleep 30
```

**Verify**:
1. Grafana login works
2. Datasource still configured
3. Dashboard still exists
4. Historical metrics preserved (check Prometheus graphs)

**Expected**: All data retained

---

## 10. Common Issues

### Port already in use
```bash
# Find process using port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                  # Linux/Mac

# Stop docker compose and retry
docker compose down
docker compose up -d
```

### Containers unhealthy
```bash
# Check logs
docker logs java-app
docker logs prometheus
docker logs grafana

# Restart specific service
docker compose restart app
```

### Metrics not updating
```bash
# Verify prometheus scrape config
docker exec prometheus cat /etc/prometheus/prometheus.yml

# Check prometheus logs
docker logs prometheus | grep scrape
```

### Grafana provisioning failed
```bash
# Check provisioning logs
docker logs grafana | grep -i provision

# Verify files mounted correctly
docker exec grafana ls /etc/grafana/provisioning/datasources
docker exec grafana ls /etc/grafana/provisioning/dashboards
```

---

## Success Criteria

**All checks must pass**:
- [x] ✅ No compilation errors
- [x] ✅ All Docker services healthy
- [x] ✅ App health endpoint returns 200
- [x] ✅ Metrics endpoint returns Prometheus format
- [x] ✅ Prometheus scrapes app every 10s
- [x] ✅ Grafana datasource auto-configured
- [x] ✅ Dashboard auto-loaded with working panels
- [x] ✅ Metrics persist after restart
- [x] ✅ Load test shows metrics updating

**Stack is fully operational** ✅
