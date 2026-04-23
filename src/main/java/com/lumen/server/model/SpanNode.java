package com.lumen.server.model;

import com.lumen.server.model.Span;
import java.util.List;
import lombok.Setter;
import lombok.Getter;

/*
    This class spanNode is not to store the spans
    This is defined to represent the span for reconstruction and latency Analysis
    -- This is the tree node — different from Span
    -- Span is raw data, SpanNode is the tree structure
*/


@Getter
@Setter
public class SpanNode {
    private Span span;
    private List<Span> Childrens;
    private Span parentSpan;

    // Methods
    
}
