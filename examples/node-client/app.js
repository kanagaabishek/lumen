const { NodeSDK } = require("@opentelemetry/sdk-node");
const { OTLPTraceExporter } = require("@opentelemetry/exporter-trace-otlp-grpc");
const { resourceFromAttributes } = require("@opentelemetry/resources");
const { SemanticResourceAttributes } = require("@opentelemetry/semantic-conventions");
const { credentials } = require("@grpc/grpc-js");
const { trace, context } = require("@opentelemetry/api");

// Point to your Lumen instance
const exporter = new OTLPTraceExporter({
  url: process.env.LUMEN_ENDPOINT || "http://localhost:9090",
  credentials: credentials.createInsecure(),
});

const sdk = new NodeSDK({
  resource: resourceFromAttributes({
    [SemanticResourceAttributes.SERVICE_NAME]: "checkout-service",
  }),
  traceExporter: exporter,
});

sdk.start();
const tracer = trace.getTracer("checkout-service");

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function simulateCheckout(orderId) {
  console.log(`\nSimulating checkout for order: ${orderId}`);

  const rootSpan = tracer.startSpan("POST /checkout");
  const rootCtx = trace.setSpan(context.active(), rootSpan);

  await context.with(rootCtx, async () => {
    // 1. Validate user session — fast
    const authSpan = tracer.startSpan("validate-session", {}, rootCtx);
    await sleep(12);
    authSpan.end();

    // 2. Check inventory — this is the bottleneck
    const inventorySpan = tracer.startSpan("check-inventory", {}, rootCtx);
    const inventoryCtx = trace.setSpan(rootCtx, inventorySpan);

    await context.with(inventoryCtx, async () => {
      // Slow DB query — this is what Lumen should detect
      const dbSpan = tracer.startSpan(
        "SELECT * FROM inventory WHERE product_id = ?",
        {},
        inventoryCtx
      );
      await sleep(280); // intentionally slow
      dbSpan.end();
    });
    inventorySpan.end();

    // 3. Gap here — simulates network wait between services
    await sleep(45);

    // 4. Process payment — two sequential operations
    const paymentSpan = tracer.startSpan("process-payment", {}, rootCtx);
    const paymentCtx = trace.setSpan(rootCtx, paymentSpan);

    await context.with(paymentCtx, async () => {
      const fraudSpan = tracer.startSpan("fraud-check", {}, paymentCtx);
      await sleep(65);
      fraudSpan.end();

      const chargeSpan = tracer.startSpan("charge-card", {}, paymentCtx);
      await sleep(90);
      chargeSpan.end();
    });
    paymentSpan.end();

    // 5. Send confirmation email — fast, runs last
    const emailSpan = tracer.startSpan("send-confirmation-email", {}, rootCtx);
    await sleep(18);
    emailSpan.end();
  });

  rootSpan.end();

  // Return the trace ID so we can query it
  const traceId = rootSpan.spanContext().traceId;
  console.log(`Trace ID: ${traceId}`);
  return traceId;
}

async function main() {
  const traceId = await simulateCheckout("order-" + Date.now());

  // Wait for exporter to flush to Lumen
  console.log("\nWaiting for spans to flush...");
  await sleep(3000);

  await sdk.shutdown();

  // Print the curl commands to query Lumen
  console.log("\n--- Query Lumen ---");
  console.log(`\n1. Get full trace with analysis:`);
  console.log(`curl http://localhost:8080/api/traces/${traceId}`);

  console.log(`\n2. List all traces for checkout-service:`);
  const from = Date.now() - 3_600_000;
  const to = Date.now() + 60_000;
  console.log(
    `curl "http://localhost:8080/api/traces?service=checkout-service&from=${from}&to=${to}"`
  );

  console.log(`\n3. List all services:`);
  console.log(`curl http://localhost:8080/api/services`);

  console.log(`\n4. Check metrics:`);
  console.log(`curl http://localhost:8080/api/metrics`);
}

main().catch(console.error);
