package com.lumen.server.domain;


public class Gap {
    
    private String beforeSpanId;
    private String afterSpanId;
    private String parentSpanId;
    private long gapMs;
    
    // Contructor
    public Gap(String beforeSpanId, String afterSpanId, String parentSpanId, long gapMs) {
        this.beforeSpanId = beforeSpanId;
        this.afterSpanId = afterSpanId;
        this.parentSpanId = parentSpanId;
        this.gapMs = gapMs;
    }
    public Gap() {}

    // Getters and Setters
    public String getBeforeSpanId() {
        return beforeSpanId;
    }
    public void setBeforeSpanId(String beforeSpanId) {
        this.beforeSpanId = beforeSpanId;
    }
    public String getAfterSpanId() {
        return afterSpanId;
    }
    public void setAfterSpanId(String afterSpanId) {
        this.afterSpanId = afterSpanId;
    }
    public String getParentSpanId() {
        return parentSpanId;
    }
    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }
    public long getGapMs() {
        return gapMs;
    }
    public void setGapMs(long gapMs) {
        this.gapMs = gapMs;
    }
}
