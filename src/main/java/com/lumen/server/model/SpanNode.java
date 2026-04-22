package com.lumen.server.model;

import com.lumen.server.model.Span;
import java.util.List;

/*
    This class spanNode is not to store the spans
    This is defined to represent the span for reconstruction and latency Analysis
    -- This is the tree node — different from Span
    -- Span is raw data, SpanNode is the tree structure
*/


public class SpanNode {
    private Span span;
    private List<Span> Childrens;
    private Span parentSpan;

    
}
