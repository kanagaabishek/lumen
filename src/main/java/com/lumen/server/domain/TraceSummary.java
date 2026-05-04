package com.lumen.server.domain;
/*
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
*/
public class TraceSummary {
    // Trace Summary Class with trace_index fields
    private String serviceName;
    private long bucket;
    private long startTimeMs;
    private String traceId;
    private long durationMs;
    private boolean hasError;
    private String rootOperation;

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    public long getBucket() {
        return bucket;
    }
    public void setBucket(long bucket) {
        this.bucket = bucket;
    }
    public long getStartTimeMs() {
        return startTimeMs;
    }
    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }
    public String getTraceId() {
        return traceId;
    }
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    public long getDurationMs() {
        return durationMs;
    }
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    public boolean isHasError() {
        return hasError;
    }
    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
    public String getRootOperation() {
        return rootOperation;
    }
    public void setRootOperation(String rootOperation) {
        this.rootOperation = rootOperation;
    }
}
