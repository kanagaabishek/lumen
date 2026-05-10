package com.lumen.server.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.lumen.server.domain.Span;
import com.lumen.server.domain.TraceSummary;

@Repository
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true", matchIfMissing = false)
public class CassandraSpanRepository implements SpanRepository {

    private final CqlSession session;
    private final PreparedStatement insertSpan;
    private final PreparedStatement insertTraceIndex;
    private final PreparedStatement findByTraceId;
    private final PreparedStatement findByServiceBucket;
    private final PreparedStatement insertService;
    private final PreparedStatement findAllServices;

    public CassandraSpanRepository(CqlSession session) {
        this.session = session;
        this.insertSpan = session.prepare(
            "INSERT INTO lumen.spans " +
            "(trace_id, span_id, parent_span_id, service_name, operation_name, " +
            "start_time_nano, end_time_nano, duration_ms, attributes, has_error) " +
            "VALUES (:trace_id, :span_id, :parent_span_id, :service_name, :operation_name, " +
            ":start_time_nano, :end_time_nano, :duration_ms, :attributes, :has_error)"
        );
        this.insertTraceIndex = session.prepare(
            "INSERT INTO lumen.trace_index " +
            "(service_name, bucket, start_time_ms, trace_id, duration_ms, has_error, root_operation) " +
            "VALUES (:service_name, :bucket, :start_time_ms, :trace_id, :duration_ms, :has_error, :root_operation)"
        );
        this.findByTraceId = session.prepare("SELECT * FROM lumen.spans WHERE trace_id = ?");
        this.findByServiceBucket = session.prepare(
            "SELECT service_name, bucket, start_time_ms, trace_id, duration_ms, has_error, root_operation " +
            "FROM lumen.trace_index " +
            "WHERE service_name = ? AND bucket = ? " +
            "AND start_time_ms >= ? AND start_time_ms <= ?"
        );

        this.insertService = session.prepare(
            "INSERT INTO lumen.services (service_name, last_seen_ms) VALUES (?, ?)"
        );
        this.findAllServices = session.prepare(
            "SELECT service_name FROM lumen.services"
        );
    }

    @Override
    public void save(Span span) {
        Map<String, String> attributes = span.getAttributes() != null ? span.getAttributes() : new HashMap<>();

        BoundStatement boundSpan = insertSpan.bind()
            .setString("trace_id", span.getTraceId())
            .setString("span_id", span.getSpanId())
            .setString("parent_span_id", span.getParentSpanId())
            .setString("service_name", span.getServiceName())
            .setString("operation_name", span.getOperationName())
            .setLong("start_time_nano", span.getStartTimeByNano())
            .setLong("end_time_nano", span.getEndTimeNano())
            .setLong("duration_ms", (span.getEndTimeNano() - span.getStartTimeByNano()) / 1_000_000)
            .setMap("attributes", attributes, String.class, String.class)
            .setBoolean("has_error", span.isHasError());

        // Always execute the span insert first
        session.execute(boundSpan);

        // Write to trace_index if this is a root span
        if (span.getParentSpanId() == null || span.getParentSpanId().isEmpty()) {
            long bucket = span.getStartTimeByNano() / (3_600_000_000_000L); // bucket by hour

            BoundStatement boundIndex = insertTraceIndex.bind()
                .setString("service_name", span.getServiceName())
                .setLong("bucket", bucket)
                .setLong("start_time_ms", span.getStartTimeByNano() / 1_000_000)
                .setString("trace_id", span.getTraceId())
                .setLong("duration_ms", (span.getEndTimeNano() - span.getStartTimeByNano()) / 1_000_000)
                .setBoolean("has_error", span.isHasError())
                .setString("root_operation", span.getOperationName());

            session.execute(boundIndex);
        }

        saveService(span.getServiceName(), span.getStartTimeByNano() / 1_000_000);
    }

    @Override
    public List<Span> findByTraceId(String traceId) {
        var resultSet = session.execute(this.findByTraceId.bind(traceId));

        return resultSet.all().stream()
            .map(row -> mapRowToSpan(row))
            .toList();
    }

    @Override
    public List<TraceSummary> findByServiceAndTimeWindow(String serviceName, long fromMs, long toMs) {
        List<TraceSummary> results = new ArrayList<>();

        long bucketFrom = fromMs / 3_600_000L;
        long bucketTo = toMs / 3_600_000L;

        for (long bucket = bucketFrom; bucket <= bucketTo; bucket++) {
            var resultSet = session.execute(findByServiceBucket.bind(serviceName, bucket, fromMs, toMs));
            resultSet.forEach(row -> results.add(mapRowToTraceSummary(row)));
        }

        return results;
    }

    private Span mapRowToSpan(com.datastax.oss.driver.api.core.cql.Row row) {
        Span span = new Span();
        span.setTraceId(row.getString("trace_id"));
        span.setSpanId(row.getString("span_id"));
        span.setParentSpan(row.getString("parent_span_id"));
        span.setServiceName(row.getString("service_name"));
        span.setOperationName(row.getString("operation_name"));
        span.setStartTimeNano(row.getLong("start_time_nano"));
        span.setEndTimeNano(row.getLong("end_time_nano"));
        span.setAttributes(row.getMap("attributes", String.class, String.class));
        span.setHasError(row.getBoolean("has_error"));
        return span;
    }

    private TraceSummary mapRowToTraceSummary(com.datastax.oss.driver.api.core.cql.Row row) {
        TraceSummary summary = new TraceSummary();
        summary.setServiceName(row.getString("service_name"));
        summary.setBucket(row.getLong("bucket"));
        summary.setStartTimeMs(row.getLong("start_time_ms"));
        summary.setTraceId(row.getString("trace_id"));
        summary.setDurationMs(row.getLong("duration_ms"));
        summary.setHasError(row.getBoolean("has_error"));
        summary.setRootOperation(row.getString("root_operation"));
        return summary;
    }

    @Override
    public void saveService(String serviceName, long timestampMs) {
        session.execute(insertService.bind(serviceName, timestampMs));
    }

    @Override
    public List<String> findAllServices() {
        var resultSet = session.execute(findAllServices.bind());
        List<String> services = new ArrayList<>();
        resultSet.forEach(row -> services.add(row.getString("service_name")));
        return services;
    }
}