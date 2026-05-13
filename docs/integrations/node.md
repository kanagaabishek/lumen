# Node.js Integration

## Install Dependencies

```bash
npm install @opentelemetry/sdk-node \
            @opentelemetry/exporter-trace-otlp-grpc \
            @opentelemetry/resources \
            @opentelemetry/semantic-conventions \
            @opentelemetry/auto-instrumentations-node \
            @grpc/grpc-js
```

## Create tracing.js

```javascript
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc');
const { resourceFromAttributes } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { credentials } = require('@grpc/grpc-js');

const sdk = new NodeSDK({
    resource: resourceFromAttributes({
        [SemanticResourceAttributes.SERVICE_NAME]: 'your-service-name',
    }),
    traceExporter: new OTLPTraceExporter({
        url: process.env.LUMEN_ENDPOINT || 'http://localhost:9090',
        credentials: credentials.createInsecure(),
    }),
    instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();
process.on('SIGTERM', () => sdk.shutdown());
process.on('SIGINT',  () => sdk.shutdown());
```

## Start Your App

```bash
# Before
node app.js

# After — no changes to app.js
node -r ./tracing.js app.js
```

## What Gets Auto-Instrumented

- Every HTTP request (http, express, fastify, koa)
- Every outbound HTTP/HTTPS call
- Every database query (pg, mysql, mongodb, redis)

## Add Custom Spans

```javascript
const { trace, context } = require('@opentelemetry/api');
const tracer = trace.getTracer('your-service-name');

async function processOrder(orderId) {
    const span = tracer.startSpan('process-order');
    try {
        // your logic
        await doSomething();
    } finally {
        span.end();
    }
}
```

## Working Example

See [examples/node-client/](../../examples/node-client/) for a 
complete checkout simulation showing bottleneck detection.