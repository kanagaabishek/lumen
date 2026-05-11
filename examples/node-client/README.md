# Node.js Checkout Example

Simulates a realistic checkout flow with multiple spans. Lumen will identify the inventory DB query as the bottleneck (280ms out of ~510ms total).

## What This Example Shows

A complete e-commerce checkout flow with:

1. **validate-session** (12ms) — Authentication check
2. **check-inventory** → **SELECT query** (280ms) — **BOTTLENECK** ← Lumen detects this
3. Gap (45ms) — Network latency between services
4. **process-payment**:
   - **fraud-check** (65ms)
   - **charge-card** (90ms)
5. **send-confirmation-email** (18ms)

**Total trace duration**: ~510ms

Lumen will show:
- ✓ Critical path: checkout → inventory → SELECT query
- ✓ Bottleneck: SELECT query at 280ms (54% of total)
- ✓ Gap: 45ms between inventory and payment

## Setup

```bash
npm install --legacy-peer-deps
```

(The `--legacy-peer-deps` flag is needed due to peer dependency constraints in OpenTelemetry packages.)

Make sure Lumen is running first:
```bash
# From project root
docker-compose up
```

## Run

```bash
npm start
```

You'll see output like:
```
Simulating checkout for order: order-1715420000000
Trace ID: 4bf92f3577b34da6a3ce929d0e0e4736

Waiting for spans to flush...

--- Query Lumen ---

1. Get full trace with analysis:
curl http://localhost:8080/api/traces/4bf92f3577b34da6a3ce929d0e0e4736

2. List all traces for checkout-service:
curl "http://localhost:8080/api/traces?service=checkout-service&from=1715416400000&to=1715420060000"

3. List all services:
curl http://localhost:8080/api/services

4. Check metrics:
curl http://localhost:8080/api/metrics
```

## View Results

Copy the curl command from the output and run it:

```bash
curl http://localhost:8080/api/traces/4bf92f3577b34da6a3ce929d0e0e4736
```

Expected response:
```json
{
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "totalDurationMs": 512,
  "criticalPathSpanIds": [
    "rootSpanId",
    "inventorySpanId",
    "dbQuerySpanId"
  ],
  "bottleneckSpanId": "dbQuerySpanId",
  "bottleneckSelfTimeMs": 280,
  "gaps": [
    {
      "afterSpanId": "inventorySpanId",
      "beforeSpanId": "paymentSpanId",
      "parentSpanId": "rootSpanId",
      "gapMs": 45
    }
  ]
}
```

## What Lumen Shows

This example demonstrates Lumen's core capabilities:

- ✓ **Bottleneck detection** — SELECT query is 54% of total duration
- ✓ **Critical path** — The chain of operations that determined total time
- ✓ **Gap detection** — The 45ms wait between inventory and payment
- ✓ **Span tree** — Parent-child relationships showing call flow
- ✓ **Self-time vs total-time** — Each span's own duration vs inclusive duration

## Key Files

- `app.js` — The checkout simulation (no dependencies on Express)
- `package.json` — OpenTelemetry dependencies
- `instrumentation.js` — SDK setup (can be removed, SDK is initialized in app.js)

## Customize

**Change Lumen endpoint:**
```bash
LUMEN_ENDPOINT=http://your-server:9090 npm start
```

**Change bottleneck duration:**
Edit line in `app.js`:
```javascript
await sleep(280); // Change this to simulate slower/faster operations
```

**Add more operations:**
```javascript
const span = tracer.startSpan("operation-name", {}, parentContext);
await sleep(100);
span.end();
```
