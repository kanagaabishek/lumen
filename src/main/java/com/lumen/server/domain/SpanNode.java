package com.lumen.server.domain;

import java.util.ArrayList;
import java.util.List;

/*
    This class spanNode is not to store the spans
    This is defined to represent the span for reconstruction and latency Analysis
    -- This is the tree node — different from Span
    -- Span is raw data, SpanNode is the tree structure
*/


public class SpanNode {
    private Span span;
    private List<SpanNode> childrens = new ArrayList<>();
    private long selfTimeMs;


    // Constructor
    public SpanNode() {}

    public SpanNode(Span span) {
        this.span = span;
    }
    public SpanNode(Span span, List<SpanNode> childrens, long selfTimeMs) {
        this.span = span;
        this.childrens = childrens;
        this.selfTimeMs = selfTimeMs;
    }
    
    // Getter and Setter
    public Span getSpan() {
        return span;
    }
    public void setSpan(Span span) {
        this.span = span;
    }
    public List<SpanNode> getChildrens() {
        return childrens;
    }
    public void setChildrens(List<SpanNode> childrens) {
        this.childrens = childrens;
    }
    public String getParentSpan() {
        return span.getParentSpanId();
    }
    public long getSelfTimeMs() {
        return selfTimeMs;
    }
    public void setSelfTimeMs(long selfTimeMs) {
        this.selfTimeMs = selfTimeMs;
    }
    
}
