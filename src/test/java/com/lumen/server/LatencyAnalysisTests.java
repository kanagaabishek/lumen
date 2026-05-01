package com.lumen.server;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.lumen.server.analysis.LatencyAnalysisService;
import com.lumen.server.domain.Span;
import com.lumen.server.domain.SpanNode;

public class LatencyAnalysisTests {
    @Test
    public void testMergeIntervalsNoOverlap() {
        // [[10,50], [60,80]] → [[10,50], [60,80]]
        List<long[]> intervals = List.of(new long[]{10,50}, new long[]{60,80});
        LatencyAnalysisService service = new LatencyAnalysisService();
        List<long[]> merged = service.mergeIntervals(intervals);
        assertEquals(2, merged.size());
        assertArrayEquals(new long[]{10,50}, merged.get(0),"First interval is incorrect");
        assertArrayEquals(new long[]{60,80}, merged.get(1),"Second interval is incorrect");
    }

    @Test
    public void testMergeIntervalsWithOverlap() {
        // [[10,50], [30,80]] → [[10,80]]
        List<long[]> intervals = List.of(new long[]{10,50}, new long[]{30,80});
        LatencyAnalysisService service = new LatencyAnalysisService();
        List<long[]> merged = service.mergeIntervals(intervals);
        assertEquals(1, merged.size());
        assertArrayEquals(new long[]{10,80}, merged.get(0),"Merged interval is incorrect");
    }

    @Test
    public void testMergeIntervalsOneInsideAnother() {
        // [[10,80], [20,40]] → [[10,80]]
        List<long[]> intervals = List.of(new long[]{10,80}, new long[]{20,40});
        LatencyAnalysisService service = new LatencyAnalysisService();
        List<long[]> merged = service.mergeIntervals(intervals);
        assertEquals(1, merged.size());
        assertArrayEquals(new long[]{10,80}, merged.get(0),"Merged interval is incorrect");
    }

    @Test
    public void testMergeIntervalsChain() {
        // [[10,50], [30,60], [55,90]] → [[10,90]]
        List<long[]> intervals = List.of(new long[]{10,50}, new long[]{30,60}, new long[]{55,90});
        LatencyAnalysisService service = new LatencyAnalysisService();
        List<long[]> merged = service.mergeIntervals(intervals);
        assertEquals(1, merged.size());
        assertArrayEquals(new long[]{10,90}, merged.get(0),"Merged interval is incorrect");
    }

    @Test
    public void testCalculateSelfTimeLeafNode() {
        // single span, no children
        // selfTime should equal duration
        Span span = new Span("span-1", "trace-1", null, "checkout", "POST /buy", 0L, 1000000L, null, false);
        SpanNode node = new SpanNode();
        node.setSpan(span);
        LatencyAnalysisService service = new LatencyAnalysisService();
        service.calculateSelfTime(node);
        assertEquals(1000, node.getSelfTimeMs(), "Self time should equal duration for leaf node");
    }

    @Test
    public void testCalculateSelfTimeWithOverlappingChildren() {
        // parent [0, 100ms]
        // child-A [10ms, 50ms], child-B [30ms, 80ms]
        // merged = [10,80] = 70ms covered
        // selfTime = 30ms
        Span parentSpan = new Span("span-1", "trace-1", null, "checkout", "POST /buy", 0L, 100_00_00L, null, false);
        Span childASpan = new Span("span-2", "trace-1", "span-1", "inventory", "GET /stock", 100_00_000L, 40000000L, null, false);
        Span childBSpan = new Span("span-3", "trace-1", "span-1", "payment", "POST /pay", 30000000L, 80000000L, null, false);
        SpanNode parentNode = new SpanNode();
        parentNode.setSpan(parentSpan);
        SpanNode childANode = new SpanNode();
        childANode.setSpan(childASpan);
        SpanNode childBNode = new SpanNode();
        childBNode.setSpan(childBSpan);
        parentNode.setChildrens(List.of(childANode, childBNode));
        LatencyAnalysisService service = new LatencyAnalysisService();
        service.calculateSelfTime(parentNode);
        assertEquals(30, parentNode.getSelfTimeMs(), "Self time should be 30ms considering overlapping children");
        
    }

    @Test
    public void testCalculateSelfTimeChildTakesFullDuration() {
        // parent [0, 100ms], child [0, 100ms]
        // selfTime = 0  (not negative)
        Span parentSpan = new Span("span-1", "trace-1", null, "checkout", "POST /buy", 0L, 1000000L, null, false);
        Span childSpan = new Span("span-2", "trace-1", "span-1", "inventory", "GET /stock", 0L, 1000000L
        , null, false);
        SpanNode parentNode = new SpanNode();
        parentNode.setSpan(parentSpan);
        SpanNode childNode = new SpanNode();
        childNode.setSpan(childSpan);
        parentNode.setChildrens(List.of(childNode));
        LatencyAnalysisService service = new LatencyAnalysisService();
        service.calculateSelfTime(parentNode);
        assertEquals(0, parentNode.getSelfTimeMs(), "Self time should be 0 when child takes full duration");
    }
}
