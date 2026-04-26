package com.lumen.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.lumen.server.analysis.TraceReconstructionService;
import com.lumen.server.domain.Span;
import com.lumen.server.domain.SpanNode;

@SpringBootTest
public class BuildTreeTest {
    @Test
    public void testBuildTreeWithSingleSpan() {
        // Test 1 — single span, no parent
        TraceReconstructionService Traceservice = new TraceReconstructionService();
        Span root = new Span("span-1", "trace-1", null, "checkout", 
        "POST /buy", 0L, 1000000L, null, false);
        List<Span> spans = List.of(root);
        SpanNode tree = Traceservice.buildTree(spans);

        // tree should not be null
        assertNotEquals(tree, null, "Tree should not be null");
        // tree.getSpan().getSpanId() should equal "span-1"
        assertEquals("span-1", tree.getSpan().getSpanId(), "SpanId is not same");
        System.out.println("SpanId: " + tree.getSpan().getSpanId());
        // tree.getChildren() should be empty
        assertTrue(tree.getChildrens().isEmpty());
        assertNotNull(tree);
    }
    // Test 2 — parent and child in order
    @Test
    public void testParentAndChildOrder(){
        TraceReconstructionService Traceservice = new TraceReconstructionService();
        Span parent = new Span("span-1", "trace-1", null, "checkout", 
        "POST /buy", 0L, 1000000L, null, false);
        Span child  = new Span("span-2", "trace-1", "span-1", "checkout", 
        "POST /buy", 20000L, 1000000L, null, false);
        SpanNode tree = Traceservice.buildTree(List.of(parent,child));
        // tree.getChildren().size() should be 1
        assertEquals(1, tree.getChildrens().size());
        // tree.getChildren().get(0).getSpan().getSpanId() should be "span-2"
        assertEquals("span-2", tree.getChildrens().get(0).getSpan().getSpanId());
        assertEquals("span-1", tree.getChildrens().get(0).getParentSpan());
    }

    // Test 3 — child arrives before parent in the list
    @Test
    public void testChildBeforeParent(){
        TraceReconstructionService Traceservice = new TraceReconstructionService();
        Span child  = new Span("span-2", "trace-1", "span-1", "inventory", "GET /stock", 20000L, 1000000L, null, false);
        Span parent = new Span("span-1", "trace-1", null, "checkout", "POST /buy", 0L, 1000000L, null, false);
        SpanNode tree = Traceservice.buildTree(List.of(child, parent)); // child first
        // should still build the same tree as Test 2
        assertEquals(1, tree.getChildrens().size());
        assertEquals("span-2", tree.getChildrens().get(0).getSpan().getSpanId());
        assertEquals("span-1", tree.getChildrens().get(0).getParentSpan());
        // should produce same result as Test 2
    }
    // Test 4 — orphan span (parentSpanId not in list)
    @Test
    public void testOrphanSpan(){
        TraceReconstructionService Traceservice = new TraceReconstructionService();
    Span orphan = new Span("span-2", "trace-1", "span-99", "inventory", "GET /stock", 20000L, 1000000L, null, false);
    Span root   = new Span("span-1", "trace-1", null, "checkout", "POST /buy", 0L, 1000000L, null, false);
    SpanNode tree = Traceservice.buildTree(List.of(orphan, root));

    // should not crash
    // root should still be span-1
    // orphan is silently ignored for now
    assertEquals("span-1", tree.getSpan().getSpanId());
    assertEquals(0, tree.getChildrens().size());
    }

    // Test 5 — three levels deep
    @Test
    public void testThreeLevelsDeep(){
        TraceReconstructionService Traceservice = new TraceReconstructionService();
        Span root  = new Span("span-1", "trace-1", null,     "checkout",  "POST /buy", 0L, 1000000L, null, false);
        Span mid   = new Span("span-2", "trace-1", "span-1", "inventory", "GET /stock", 20000L, 1000000L, null, false);
        Span leaf  = new Span("span-3", "trace-1", "span-2", "postgres",  "SELECT", 30000L, 1000000L, null, false);
        SpanNode tree = Traceservice.buildTree(List.of(root, mid, leaf));
        // tree.getChildren().get(0).getChildren().get(0).getSpan().getSpanId() 
        assertEquals("span-3", tree.getChildrens().get(0).getChildrens().get(0).getSpan().getSpanId());
        // should be "span-3"
    }
}
