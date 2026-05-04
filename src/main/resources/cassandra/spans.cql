CREATE TABLE IF NOT EXISTS lumen.spans (
    trace_id        TEXT,
    span_id         TEXT,
    parent_span_id  TEXT,
    service_name    TEXT,
    operation_name  TEXT,
    start_time_nano BIGINT,
    end_time_nano   BIGINT,
    duration_ms     BIGINT,
    attributes      MAP<TEXT, TEXT>,
    has_error       BOOLEAN,
    status          TEXT,
    PRIMARY KEY (trace_id, start_time_nano, span_id)
) WITH CLUSTERING ORDER BY (start_time_nano ASC, span_id ASC)
    AND default_time_to_live = 604800
    AND comment = 'Stores individual spans from distributed traces. Partitioned by trace_id for co-location of related spans.';