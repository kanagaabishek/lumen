# Java / Spring Boot Integration

Connect your Spring Boot app to Lumen using the 
OpenTelemetry Java Agent. Zero code changes required.

## Step 1 — Download the Agent

```bash
curl -L https://github.com/open-telemetry/opentelemetry-java-instrumentation\
/releases/latest/download/opentelemetry-javaagent.jar \
  -o opentelemetry-javaagent.jar
```

## Step 2 — Run With the Agent

```bash
java -javaagent:./opentelemetry-javaagent.jar \
  -Dotel.service.name=your-service-name \
  -Dotel.exporter.otlp.endpoint=http://localhost:9090 \
  -Dotel.exporter.otlp.protocol=grpc \
  -Dotel.traces.exporter=otlp \
  -Dotel.metrics.exporter=none \
  -Dotel.logs.exporter=none \
  -jar target/your-app.jar
```

## Or via application.properties

```properties
otel.service.name=your-service-name
otel.exporter.otlp.endpoint=http://localhost:9090
otel.exporter.otlp.protocol=grpc
otel.traces.exporter=otlp
otel.metrics.exporter=none
otel.logs.exporter=none
```

Then run:
```bash
java -javaagent:./opentelemetry-javaagent.jar -jar target/your-app.jar
```

## What Gets Auto-Instrumented

- Every HTTP request → root span
- Every outbound HTTP call → child span
- Every JDBC query → child span showing the SQL
- Spring Cache, Spring Scheduling, Spring Data

## Verify

```bash
# Hit any endpoint on your app
curl http://localhost:{your-port}/your-endpoint

# Check Lumen
curl http://localhost:8080/api/services
# Should show your service name
```