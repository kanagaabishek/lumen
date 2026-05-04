CREATE TABLE IF NOT EXISTS lumen.trace_index (
    service_name    TEXT,
    bucket          BIGINT,
    start_time_ms   BIGINT,
    trace_id        TEXT,
    duration_ms     BIGINT,
    has_error       BOOLEAN,
    root_operation  TEXT,
    PRIMARY KEY ((service_name, bucket), start_time_ms, trace_id)
) WITH CLUSTERING ORDER BY (start_time_ms DESC, trace_id ASC)
    AND default_time_to_live = 604800;