# Python Integration

## Install Dependencies

```bash
pip install opentelemetry-sdk \
            opentelemetry-exporter-otlp-proto-grpc \
            opentelemetry-instrumentation-flask \
            opentelemetry-instrumentation-requests \
            opentelemetry-instrumentation-sqlalchemy
```

## Option A — Auto-instrument (zero code changes)

```bash
# Install the auto-instrumentation agent
pip install opentelemetry-distro
opentelemetry-bootstrap -a install

# Run your app
OTEL_SERVICE_NAME=your-service-name \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:9090 \
OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
OTEL_TRACES_EXPORTER=otlp \
OTEL_METRICS_EXPORTER=none \
OTEL_LOGS_EXPORTER=none \
opentelemetry-instrument python app.py
```

## Option B — Manual setup

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource

resource = Resource(attributes={"service.name": "your-service-name"})
provider = TracerProvider(resource=resource)
provider.add_span_processor(
    BatchSpanProcessor(
        OTLPSpanExporter(
            endpoint="http://localhost:9090",
            insecure=True
        )
    )
)
trace.set_tracer_provider(provider)
```

## Flask Example

```python
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from flask import Flask

app = Flask(__name__)
FlaskInstrumentor().instrument_app(app)

@app.route('/health')
def health():
    return {'status': 'ok'}
```

Every request to `/health` automatically creates a span in Lumen.

## Add Custom Spans

```python
tracer = trace.get_tracer(__name__)

def process_payment(amount):
    with tracer.start_as_current_span("process-payment") as span:
        span.set_attribute("payment.amount", amount)
        # your logic here
        return charge_card(amount)
```

## Verify

```bash
curl http://localhost:8080/api/services
# Should show your-service-name
```