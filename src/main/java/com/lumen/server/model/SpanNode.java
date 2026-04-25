package com.lumen.server.model;

import java.util.List;

/*
    This class spanNode is not to store the spans
    This is defined to represent the span for reconstruction and latency Analysis
    -- This is the tree node — different from Span
    -- Span is raw data, SpanNode is the tree structure
*/


public class SpanNode {
    private Span span;
    private List<SpanNode> Childrens;
    private Span parentSpan;

    // Getter and Setter
    public Span getSpan() {
        return span;
    }
    public void setSpan(Span span) {
        this.span = span;
    }
    public List<SpanNode> getChildrens() {
        return Childrens;
    }
    public void setChildrens(List<SpanNode> childrens) {
        Childrens = childrens;
    }
    public Span getParentSpan() {
        return parentSpan;
    }
    public void setParentSpan(Span parentSpan) {
        this.parentSpan = parentSpan;
    }
    
}
