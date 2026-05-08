package com.lumen.server.unit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lumen.server.analysis.LatencyAnalysisService;
import com.lumen.server.domain.Span;
import com.lumen.server.domain.SpanNode;
import com.lumen.server.domain.Gap;

public class ComputationOfTimeAndPathAndGapDetectionTests {

    private LatencyAnalysisService service = new LatencyAnalysisService();

    // Test 1 — single root, no children
    @Test
    public void testComputeCriticalTimeWithSingleRoot() {
        // Setup: span-1 (root, no children)
        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", 0, 20_000_000, null, false);
        SpanNode root = new SpanNode(rootSpan);

        service.calculateSelfTime(root);
        Map<String, String> criticalChildMap = new HashMap<>();
        long criticalTime = service.computeCriticalTime(root, criticalChildMap);

        // Assertions
        assertEquals(20, root.getSelfTimeMs(), "Root selfTime should be 20ms");
        assertEquals(20, criticalTime, "computeCriticalTime should return 20ms");
        assertEquals(0, criticalChildMap.size(), "criticalChildMap should be empty");

        // Test collectCriticalPath
        Map<String, SpanNode> nodeMap = new HashMap<>();
        nodeMap.put("span-1", root);
        List<String> criticalPath = service.collectCriticalPath(root, criticalChildMap, nodeMap);
        assertEquals(List.of("span-1"), criticalPath, "collectCriticalPath should return [span-1]");
    }

    // Test 2 — root with two children, one longer
    @Test
    public void testComputeCriticalTimeWithTwoChildren() {
        // Setup tree:
        // span-1 (root, selfTime=20ms)
        //   ├─ span-2 (child-B, selfTime=40ms, critical)
        //   └─ span-3 (child-A, selfTime=15ms)

        long rootStart = 0;
        long rootEnd = 75_000_000;  // 75ms total

        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);

        // Child-B (longer, critical): 40ms duration
        long childBStart = 35_000_000;  // 35ms into root
        long childBEnd = 75_000_000;    // 75ms (ends with root)
        Span childBSpan = new Span("span-2", "trace-1", "span-1", "service-1", "op-b", childBStart, childBEnd, null, false);
        SpanNode childB = new SpanNode(childBSpan);

        // Child-A (shorter): 15ms duration
        long childAStart = 20_000_000;  // 20ms into root
        long childAEnd = 35_000_000;    // 35ms
        Span childASpan = new Span("span-3", "trace-1", "span-1", "service-1", "op-a", childAStart, childAEnd, null, false);
        SpanNode childA = new SpanNode(childASpan);

        root.getChildrens().add(childB);
        root.getChildrens().add(childA);

        service.calculateSelfTime(root);

        // Verify self times after calculateSelfTime
        assertEquals(40, childB.getSelfTimeMs(), "Child-B selfTime should be 40ms");
        assertEquals(15, childA.getSelfTimeMs(), "Child-A selfTime should be 15ms");
        assertEquals(20, root.getSelfTimeMs(), "Root selfTime should be 20ms");

        // Compute critical time
        Map<String, String> criticalChildMap = new HashMap<>();
        long criticalTime = service.computeCriticalTime(root, criticalChildMap);

        assertEquals(60, criticalTime, "computeCriticalTime should return 60ms (20+40)");
        assertEquals("span-2", criticalChildMap.get("span-1"), "criticalChildMap should contain span-1 → span-2");

        // Test collectCriticalPath
        Map<String, SpanNode> nodeMap = new HashMap<>();
        nodeMap.put("span-1", root);
        nodeMap.put("span-2", childB);
        nodeMap.put("span-3", childA);
        List<String> criticalPath = service.collectCriticalPath(root, criticalChildMap, nodeMap);
        assertEquals(List.of("span-1", "span-2"), criticalPath, "collectCriticalPath should return [span-1, span-2]");
    }

    // Test 3 — three levels deep
    @Test
    public void testComputeCriticalTimeWithThreeLevels() {
        // Setup tree:
        // span-1 (root, selfTime=10ms)
        //   └─ span-2 (mid, selfTime=20ms)
        //        └─ span-3 (leaf, selfTime=50ms)

        long rootStart = 0;
        long rootEnd = 80_000_000;   // 80ms total

        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);

        long midStart = 10_000_000;  // 10ms into root
        long midEnd = 80_000_000;    // 80ms (ends with root)
        Span midSpan = new Span("span-2", "trace-1", "span-1", "service-1", "mid-op", midStart, midEnd, null, false);
        SpanNode mid = new SpanNode(midSpan);

        long leafStart = 30_000_000; // 30ms into root
        long leafEnd = 80_000_000;   // 80ms (ends with root)
        Span leafSpan = new Span("span-3", "trace-1", "span-2", "service-1", "leaf-op", leafStart, leafEnd, null, false);
        SpanNode leaf = new SpanNode(leafSpan);

        root.getChildrens().add(mid);
        mid.getChildrens().add(leaf);

        service.calculateSelfTime(root);

        // Verify self times
        assertEquals(50, leaf.getSelfTimeMs(), "Leaf selfTime should be 50ms");
        assertEquals(20, mid.getSelfTimeMs(), "Mid selfTime should be 20ms");
        assertEquals(10, root.getSelfTimeMs(), "Root selfTime should be 10ms");

        // Compute critical time
        Map<String, String> criticalChildMap = new HashMap<>();
        long criticalTime = service.computeCriticalTime(root, criticalChildMap);

        assertEquals(80, criticalTime, "computeCriticalTime should return 80ms (10+20+50)");
        assertEquals("span-2", criticalChildMap.get("span-1"), "Root's critical child should be span-2");
        assertEquals("span-3", criticalChildMap.get("span-2"), "Mid's critical child should be span-3");

        // Test collectCriticalPath
        Map<String, SpanNode> nodeMap = new HashMap<>();
        nodeMap.put("span-1", root);
        nodeMap.put("span-2", mid);
        nodeMap.put("span-3", leaf);
        List<String> criticalPath = service.collectCriticalPath(root, criticalChildMap, nodeMap);
        assertEquals(List.of("span-1", "span-2", "span-3"), criticalPath,
                   "collectCriticalPath should return [span-1, span-2, span-3]");
    }

    // Test 4 — verify findBottleNeck
    @Test
    public void testFindBottleNeck() {
        // Reuse Test 3 structure to verify findBottleNeck
        long rootStart = 0;
        long rootEnd = 80_000_000;

        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);

        long midStart = 10_000_000;
        long midEnd = 80_000_000;
        Span midSpan = new Span("span-2", "trace-1", "span-1", "service-1", "mid-op", midStart, midEnd, null, false);
        SpanNode mid = new SpanNode(midSpan);

        long leafStart = 30_000_000;
        long leafEnd = 80_000_000;
        Span leafSpan = new Span("span-3", "trace-1", "span-2", "service-1", "leaf-op", leafStart, leafEnd, null, false);
        SpanNode leaf = new SpanNode(leafSpan);

        root.getChildrens().add(mid);
        mid.getChildrens().add(leaf);

        service.calculateSelfTime(root);

        // Compute critical time and path
        Map<String, String> criticalChildMap = new HashMap<>();
        service.computeCriticalTime(root, criticalChildMap);

        Map<String, SpanNode> nodeMap = new HashMap<>();
        nodeMap.put("span-1", root);
        nodeMap.put("span-2", mid);
        nodeMap.put("span-3", leaf);
        List<String> criticalPath = service.collectCriticalPath(root, criticalChildMap, nodeMap);

        // Find bottleneck
        SpanNode bottleNeck = service.findBottleNeck(root, criticalPath, nodeMap);

        // Assert bottleneck is the leaf with highest selfTime (50ms)
        assertEquals(leaf, bottleNeck, "Bottleneck should be the leaf node");
        assertEquals(50, bottleNeck.getSelfTimeMs(), "Bottleneck selfTime should be 50ms");
        assertEquals("span-3", bottleNeck.getSpan().getSpanId(), "Bottleneck spanId should be span-3");
    }

    //Test for FindGaps
    @Test
    public void testDetectGapsAboveThreshold() {
        // Setup tree:
        // span-1 (root, selfTime=10ms)
        //   └─ span-2 (child, selfTime=10ms, starts 30ms after root ends → gap = 20ms)     
        long rootStart = 0;
        long rootEnd = 10_000_000;   // 10ms total
        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);
        // child-A: 10ms to 40ms
        Span childASpan = new Span("span-2", "trace-1", "span-1", 
            "service-1", "child-op-a", 10_000_000L, 40_000_000L, null, false);
        SpanNode childA = new SpanNode(childASpan);

        // child-B: 70ms to 90ms  
        // gap = 70 - 40 = 30ms ← above threshold of 15ms
        Span childBSpan = new Span("span-3", "trace-1", "span-1",
            "service-1", "child-op-b", 70_000_000L, 90_000_000L, null, false);
        SpanNode childB = new SpanNode(childBSpan);

        root.getChildrens().add(childA);
        root.getChildrens().add(childB);
        List<Gap> gaps = service.findGaps(root, 15); // threshold = 15ms
        assertEquals(1, gaps.size(), "Should detect one gap above threshold");
        Gap detectedGap = gaps.get(0);
        assertEquals("span-2", detectedGap.getBeforeSpanId());
        assertEquals("span-3", detectedGap.getAfterSpanId());
        assertEquals("span-1", detectedGap.getParentSpanId());
        assertEquals(30, detectedGap.getGapMs());
    }

    // Test 2 — gap below threshold → returns empty list
    @Test
    public void testDetectGapsBelowThreshold() {
        // Setup tree with two children (gap below threshold):
        // span-1 (root)
        //   ├─ span-2 (child-A, ends at 40ms)
        //   │                             ← 10ms gap
        //   └─ span-3 (child-B, starts at 50ms)
        long rootStart = 0;
        long rootEnd = 100_000_000;  // 100ms total
        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);

        // Child-A: 10ms to 40ms
        Span childASpan = new Span("span-2", "trace-1", "span-1", "service-1", "child-op-a", 10_000_000L, 40_000_000L, null, false);
        SpanNode childA = new SpanNode(childASpan);

        // Child-B: 50ms to 60ms (gap = 50 - 40 = 10ms, below threshold of 15ms)
        Span childBSpan = new Span("span-3", "trace-1", "span-1", "service-1", "child-op-b", 50_000_000L, 60_000_000L, null, false);
        SpanNode childB = new SpanNode(childBSpan);

        root.getChildrens().add(childA);
        root.getChildrens().add(childB);

        List<Gap> gaps = service.findGaps(root, 15); // threshold = 15ms
        assertEquals(0, gaps.size(), "Should not detect any gaps below threshold");
    }

    @Test
    public void testDetectGapsAtSecondLevel() {
        // Setup tree with gap at second level:
        // span-1 (root)
        //   └─ span-2 (child with two grandchildren)
        //        ├─ span-3 (grandchild-A, ends at 30ms)
        //        │                             ← 20ms gap
        //        └─ span-4 (grandchild-B, starts at 50ms)
        long rootStart = 0;
        long rootEnd = 100_000_000;   // 100ms total
        Span rootSpan = new Span("span-1", "trace-1", null, "service-1", "root-op", rootStart, rootEnd, null, false);
        SpanNode root = new SpanNode(rootSpan);

        // Child: 10ms to 60ms
        long childStart = 10_000_000;
        long childEnd = 60_000_000;
        Span childSpan = new Span("span-2", "trace-1", "span-1", "service-1", "child-op", childStart, childEnd, null, false);
        SpanNode child = new SpanNode(childSpan);

        // Grandchild-A: 10ms to 30ms
        Span grandChildASpan = new Span("span-3", "trace-1", "span-2", "service-1", "grandchild-op-a", 10_000_000L, 30_000_000L, null, false);
        SpanNode grandChildA = new SpanNode(grandChildASpan);

        // Grandchild-B: 50ms to 60ms (gap = 50 - 30 = 20ms)
        Span grandChildBSpan = new Span("span-4", "trace-1", "span-2", "service-1", "grandchild-op-b", 50_000_000L, 60_000_000L, null, false);
        SpanNode grandChildB = new SpanNode(grandChildBSpan);

        root.getChildrens().add(child);
        child.getChildrens().add(grandChildA);
        child.getChildrens().add(grandChildB);

        List<Gap> gaps = service.findGaps(root, 15); // threshold = 15ms
        assertEquals(1, gaps.size(), "Should detect one gap above threshold");
        Gap detectedGap = gaps.get(0);
        assertEquals("span-3", detectedGap.getBeforeSpanId(), "beforeSpanId should be span-3");
        assertEquals("span-4", detectedGap.getAfterSpanId(), "afterSpanId should be span-4");
        assertEquals("span-2", detectedGap.getParentSpanId(), "parentSpanId should be span-2");
        assertEquals(20, detectedGap.getGapMs(), "gapMs should be 20ms (50-30)");
    }
}