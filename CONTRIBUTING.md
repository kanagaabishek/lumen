
## Contributing to Lumen

Contributions are welcome! Here's how to get started developing Lumen.

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/lumen.git
   cd lumen
   ```

2. **Start Cassandra** (required for integration tests and running locally)
   ```bash
   docker run -d --name lumen-cassandra -p 9042:9042 cassandra:4.1
   ```
   
   Wait 60 seconds for Cassandra to be ready before proceeding.

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
   
   Lumen will start on:
   - REST API: http://localhost:8080
   - gRPC: http://localhost:9090

### Running Tests

**Unit tests** (no Cassandra required):
```bash
mvn test
```

**Integration tests** (requires Cassandra):
```bash
# Ensure Cassandra is running, then:
mvn test -Dtest=CassandraSpanRepositoryIT -Dcassandra.enabled=true
```

**Full build** (unit + integration tests):
```bash
mvn clean package
```

### Project Structure

```
src/main/java/com/lumen/server/
├── domain/          — Core data models (no framework dependencies)
│   ├── Span         — Raw trace span from OpenTelemetry
│   ├── SpanNode     — Tree node wrapping a Span with children
│   └── TraceBuildResult — Result of reconstructing a trace tree
│
├── analysis/        — Trace analysis algorithms
│   ├── TraceReconstructionService — Build span trees from flat list
│   └── LatencyAnalysisService     — Compute self-time and critical path
│
├── storage/         — Cassandra repository layer
│   ├── SpanRepository             — Interface for span storage
│   └── CassandraSpanRepository    — Cassandra implementation
│
├── ingestion/       — gRPC handler and async queue
│   ├── TraceServiceImpl     — gRPC service endpoint
│   ├── SpanBatchWriter     — Batch writer for Cassandra
│   ├── SpanIngestionQueue  — Thread-safe ingestion queue
│   └── SchemaInitializer   — Creates Cassandra keyspace/tables
│
├── api/             — REST controllers
│   └── TraceQueryController — HTTP endpoints for querying traces
│
├── service/         — Business logic orchestration
│   └── TraceQueryService   — Orchestrates analysis and storage
│
└── config/          — Spring configuration
    ├── CassandraConfig    — Cassandra session setup
    └── GrpcServerConfig   — gRPC server configuration
```

### Code Style & Guidelines

- **Keep it simple**: No abstractions for single-use code. No "flexibility" that isn't needed.
- **Domain models first**: Core logic (domain/, analysis/) has zero framework dependencies.
- **Test the algorithms**: Unit test domain and analysis classes without Cassandra.
- **Integration tests**: Test CassandraSpanRepository with a real Cassandra instance.
- **Comments sparingly**: Code should be self-documenting. Only comment the "why" if non-obvious.

### Key Classes to Know

**TraceReconstructionService** (`analysis/`)
- Converts a flat list of spans into a hierarchical tree
- Maps spans to parents by `spanId` and `parentSpanId`
- Drops orphan spans (parent not in the list)

**LatencyAnalysisService** (`analysis/`)
- Computes `selfTimeMs`: time a span spent in its own work (excluding children)
- Uses interval union to handle overlapping child spans
- Finds critical path (slowest chain through the tree)
- Detects bottleneck (single span with highest self-time on critical path)
- Finds gaps (idle time between operations)

**SpanIngestionQueue** (`ingestion/`)
- Thread-safe queue for span ingestion
- gRPC threads call `offer()` (non-blocking, may drop)
- Background thread drains batches every 100ms
- Dropped spans increment a counter visible at `/api/metrics`

### Making a Pull Request

1. **Fork the repo** and create a feature branch
2. **Write tests first** for any new behavior
3. **Keep commits atomic** — one logical change per commit
4. **Update the README** if you've changed APIs or behavior
5. **Run `mvn clean package`** to ensure all tests pass
6. **Open a PR** with a clear description of what and why

### Common Development Tasks

**Add a new REST endpoint:**
1. Add method to `TraceQueryController` in `api/`
2. Add service method to `TraceQueryService` in `service/`
3. Write tests in `src/test/`
4. Update API Reference section in README

**Add a new analysis metric:**
1. Add method to `LatencyAnalysisService` in `analysis/`
2. Unit test in `src/test/` (no Cassandra needed)
3. Wire up in `TraceQueryService`
4. Expose via REST endpoint or gRPC

**Optimize database queries:**
1. Edit `CassandraSpanRepository` in `storage/`
2. Test with `CassandraSpanRepositoryIT`
3. Profile with a real load (use `examples/node-client/`)

### Questions?

- Open an issue on GitHub
- Check `examples/node-client/` for a working client reference
- Read the engineering decisions in this README for context on trade-offs

