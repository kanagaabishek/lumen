package com.lumen.server.domain;

import java.util.List;

public class TraceResponse {
    private String traceId;
    private long totalDurationMs;
    private SpanNode root;
    private List<String> criticalPathSpanIds;
    private String bottleneckSpanId;
    private long bottleneckSelfTimeMs;
    private List<Gap> gaps;

    // Contructor
    public TraceResponse(String traceId, long totalDurationMs, SpanNode root, List<String> criticalPathSpanIds,
                         String bottleneckSpanId, long bottleneckSelfTimeMs, List<Gap> gaps) {
        this.traceId = traceId;
        this.totalDurationMs = totalDurationMs;
        this.root = root;
        this.criticalPathSpanIds = criticalPathSpanIds;
        this.bottleneckSpanId = bottleneckSpanId;
        this.bottleneckSelfTimeMs = bottleneckSelfTimeMs;
        this.gaps = gaps;
    }

    public TraceResponse() {
    }

    // Getters and Setters
    public String getTraceId() {
        return traceId;
    }
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    public long getTotalDurationMs() {
        return totalDurationMs;
    }
    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs; 
    }
    public SpanNode getRoot() {
        return root;
    }
    public void setRoot(SpanNode root) {
        this.root = root;
    }
    public List<String> getCriticalPathSpanIds() {
        return criticalPathSpanIds;
    }
    public void setCriticalPathSpanIds(List<String> criticalPathSpanIds) {
        this.criticalPathSpanIds = criticalPathSpanIds;
    }
    public String getBottleneckSpanId() {
        return bottleneckSpanId;
    }
    public void setBottleneckSpanId(String bottleneckSpanId) {
        this.bottleneckSpanId = bottleneckSpanId;
    }
    public long getBottleneckSelfTimeMs() {
        return bottleneckSelfTimeMs;
    }
    public void setBottleneckSelfTimeMs(long bottleneckSelfTimeMs) {
        this.bottleneckSelfTimeMs = bottleneckSelfTimeMs;
    }
    public List<Gap> getGaps() {
        return gaps;
    }
    public void setGaps(List<Gap> gaps) {
        this.gaps = gaps;
    }

}
