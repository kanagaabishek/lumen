package com.lumen.server.storage;

import java.util.ArrayList;
import java.util.List;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.springframework.stereotype.Repository;

import com.lumen.server.domain.Span;
import com.lumen.server.domain.TraceSummary;

@Repository
public class CassandraSpanRepository implements SpanRepository {

    private final CqlSession session;
    private final PreparedStatement insertSpan;
    private final PreparedStatement insertTraceIndex;
    private final PreparedStatement findByTraceId;
    private final PreparedStatement findByServiceBucket;

    public CassandraSpanRepository(CqlSession session) {
        this.session = session;
        this.insertSpan = session.prepare("INSERT INTO lumen.spans (trace_id, span_id, parent_span_id, service_name, operation_name, start_time_nano, end_time_nano, duration_ms, attributes, has_error) VALUES (?,?,?,?,?,?,?,?,?,?)");
        this.insertTraceIndex = session.prepare("INSERT INTO lumen.trace_index (service_name,bucket,start_time_ms,trace_id,duration_ms,has_error,root_operation) VALUES (?,?,?,?,?,?,?)");
        this.findByTraceId = session.prepare("SELECT * FROM lumen.spans WHERE trace_id = ?");
        this.findByServiceBucket = session.prepare(
            "SELECT service_name, bucket, start_time_ms, trace_id, duration_ms, has_error, root_operation " +
            "FROM lumen.trace_index " +
            "WHERE service_name = ? AND bucket = ? " +
            "AND start_time_ms >= ? AND start_time_ms <= ?"
        );
    }

    @Override
    public void save(Span span) {
        BatchStatement batch = BatchStatement.newInstance(BatchType.LOGGED)
            .add(insertSpan.bind(
                span.getTraceId(),
                span.getSpanId(),
                span.getParentSpanId(),
                span.getServiceName(),
                span.getOperationName(),
                span.getStartTimeByNano(),
                span.getEndTimeNano(),
                (span.getEndTimeNano() - span.getStartTimeByNano()) / 1_000_000,
                span.getAttributes().toString(),
                span.isHasError()
            ));

        if (span.getParentSpanId() == null || span.getParentSpanId().isEmpty()) {
            long bucket = span.getStartTimeByNano() / (3_600_000_000_000L); // bucket by hour
            batch.add(insertTraceIndex.bind(
                span.getServiceName(),
                bucket,
                span.getStartTimeByNano() / 1_000_000,
                span.getTraceId(),
                (span.getEndTimeNano() - span.getStartTimeByNano()) / 1_000_000,
                span.isHasError(),
                span.getOperationName()
            ));
        }

        session.execute(batch);
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
}