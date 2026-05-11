# Lumen — Distributed Trace Analysis System

## What is Lumen?

Lumen is a self-hosted distributed tracing system that collects, stores, 
and analyzes traces from your microservices. It tells you not just what 
happened in a request — but exactly where time was lost, which service 
caused the slowdown, and what the critical path through your system looks like.

## What Problem Does It Solve?

In a distributed system, a single user request can touch 10 different 
services. When that request is slow or fails, finding the root cause means 
manually correlating logs across all 10 services — which can take hours.

Lumen solves this by:
- Collecting spans from every service automatically via OpenTelemetry
- Reconstructing the full request tree showing parent-child relationships
- Computing the critical path — the chain of spans that determined total duration
- Identifying the bottleneck span — the single operation consuming the most time
- Detecting gaps — periods where a service was idle between calls

A request that took 450ms is no longer a mystery. Lumen shows you:
"74% of time was spent in inventory-service → SELECT * FROM products. 
Everything else was fast."

## Who Uses It?

Any team running microservices who wants to understand latency without 
paying for Datadog or New Relic. Lumen is self-hosted — your trace data 
never leaves your infrastructure.


# Architecture
![Architecture Diagram](<Architecture-diagram.png>)

![FLOW DIAGRAM](<Flow-diagram.png>)

## How to Run

### Prerequisites
- Docker and Docker Compose

### Start
```bash
docker-compose up
```
Cassandra starts, schema initializes automatically, Lumen starts on ports 8080 and 9090.

### Send traces from your app
```javascript
// Node.js
const exporter = new OTLPTraceExporter({ url: 'http://localhost:9090' });
```
```python
# Python
exporter = OTLPSpanExporter(endpoint="http://localhost:9090", insecure=True)
```
```java
// Java — application.properties
otel.exporter.otlp.endpoint=http://localhost:9090
```

## Engineering Decisions

### Why Cassandra over Postgres
Cassandra ensures no row-level locking, no indexes updating, and no ACID overhead. Data is stored in a memory table first, then sequentially written to disk.

Postgres, by contrast, updates indexes immediately and uses row-level locking. At 10,000 spans/second, Postgres lock contention becomes the bottleneck.

Additionally, Cassandra has built-in TTL support (specified as 7 days), whereas Postgres requires a cron job to clean up old data.


### Why two tables
In Cassandra, a secondary index on service_name in the spans table causes a scatter-gather query — Cassandra asks every node "do you have checkout spans?" At 10 nodes that's 10 network calls. At 100 nodes it's 100. Performance degrades as the cluster grows.

A separate table partitioned by service_name means one targeted partition lookup regardless of cluster size.

### Why time bucketing
Partitioning trace_index by service_name alone creates a hot partition — all checkout-service traces land on one Cassandra node while others sit idle. At 1,000 requests/hour over 30 days, that's 720,000 rows on a single node.

Bucketing by hour distributes the load: (checkout-service, hour-18500) and (checkout-service, hour-18501) are separate partitions on separate nodes. 30 days = 720 partitions spread across the cluster. Queries for "last 1 hour" touch exactly 1-2 partitions regardless of total data size.

### Why gRPC for ingestion
One span in JSON is roughly 400-600 bytes. The same span in protobuf is 100-150 bytes — about 3-4x smaller
The throughput difference is significant which makes gRPC highly scalable and efficient in handling millions of interaction per second

gRPC uses Protocol Buffers — a schema-enforced binary format. 
Malformed data fails at deserialization rather than being silently 
stored as garbage. HTTP/2 multiplexing allows multiple requests over 
one connection, unlike HTTP/1.1 which requires one connection per request.

### Why async ingestion queue
Synchronous writes mean gRPC threads block waiting for Cassandra. At 10,000 spans/second with 10ms writes, you need 100 concurrent threads just to keep up — the gRPC pool exhausts and requests fail.

LinkedBlockingQueue decouples ingestion from writes. gRPC threads call `offer()` — which returns immediately whether the queue accepts or drops the span — and move on. A single background thread drains up to 500 spans every 100ms and batch-writes to Cassandra.

When the queue is full, spans are dropped and a counter increments. This counter is visible at `/api/metrics`. The system stays responsive under load; operators see the dropped count and scale accordingly.

### Why interval union for self time
parent [10ms → 70ms] = 60ms duration
child-A [10ms → 50ms] = 40ms
child-B [30ms → 70ms] = 40ms

Services make parallel downstream calls. Naive self-time calculation 
sums child durations — which double-counts overlapping time and 
produces negative results

### Why BATCH writes
You write to two tables — spans and trace_index. What happens if the first write succeeds and the second fails due to a Cassandra node hiccup?

BATCH solves this problem by writing both atomically — either both commit or neither does. This prevents inconsistent state. Because Cassandra writes are idempotent (writing the same data twice produces the same result), retrying a failed batch is safe — no duplicates.

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | gRPC :9090/TraceService/Export | Ingest spans (OpenTelemetry format) |
| GET | /api/traces/{traceId} | Full trace with critical path and bottleneck |
| GET | /api/traces?service=X&from=Y&to=Z | List traces for a service in time window |
| GET | /api/services | List all services that have sent traces |
| GET | /api/metrics | Queue depth, ingested count, dropped count |
| GET | /api/health | Server health check |

## What's Next

**Tail-based sampling** — Currently Lumen stores 100% of traces. At production scale this is expensive. Tail-based sampling buffers spans in memory and only persists traces that were slow or errored — keeping the interesting traces and dropping the noise.

**Distributed ingestion** — Replace the in-memory queue with Kafka. Multiple Lumen instances consume from the same topic, enabling horizontal scaling of the write path.

**Web UI** — A trace viewer showing the span tree visually as a waterfall diagram, with critical path highlighted and bottleneck annotated.

**Alert rules** — Notify when p99 latency for a service exceeds a threshold for N consecutive minutes.

