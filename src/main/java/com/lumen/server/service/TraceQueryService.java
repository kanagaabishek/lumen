package com.lumen.server.service;

import com.lumen.server.storage.SpanRepository;

import java.util.HashMap;
import java.util.List;

import com.lumen.server.analysis.LatencyAnalysisService;
import com.lumen.server.analysis.TraceReconstructionService;
import com.lumen.server.domain.SpanNode;
import com.lumen.server.domain.TraceBuildResult;
import com.lumen.server.domain.TraceResponse;
import com.lumen.server.domain.Gap;
import com.lumen.server.domain.Span;
import java.util.Map;

public class TraceQueryService {
    private final SpanRepository spanRepository;
    private final LatencyAnalysisService latencyAnalysisService;
    private final TraceReconstructionService traceReconstructionService;

    public TraceQueryService(SpanRepository spanRepository, LatencyAnalysisService latencyAnalysisService, TraceReconstructionService traceReconstructionService) {
        this.spanRepository = spanRepository;
        this.latencyAnalysisService = latencyAnalysisService;
        this.traceReconstructionService = traceReconstructionService;
    }

    public TraceResponse getTrace(String traceId) {
        List<Span> spans = spanRepository.findByTraceId(traceId);
        if (spans.isEmpty()) return null;

        // Build tree
        TraceBuildResult tree = traceReconstructionService.buildTree(spans);
        Map<String, SpanNode> nodeMap = tree.getNodeMap();
        SpanNode root = tree.getRoot();

        // Analysis — order matters
        latencyAnalysisService.calculateSelfTime(root);

        Map<String, String> criticalChildMap = new HashMap<>();
        latencyAnalysisService.computeCriticalTime(root, criticalChildMap);
        // ↑ return value not needed — side effect fills criticalChildMap

        List<String> criticalPath = latencyAnalysisService
            .collectCriticalPath(root, criticalChildMap, nodeMap);

        SpanNode bottleNeck = latencyAnalysisService
            .findBottleNeck(root, criticalPath, nodeMap);

        List<Gap> gaps = latencyAnalysisService.findGaps(root, 10);

        // Compute true total duration
        long trueEnd = spans.stream()
            .mapToLong(Span::getEndTimeNano)
            .max()
            .orElse(0L);
        long totalDurationMs = (trueEnd - root.getSpan().getStartTimeByNano()) 
                            / 1_000_000;

        // Build response
        TraceResponse response = new TraceResponse();
        response.setTraceId(traceId);
        response.setTotalDurationMs(totalDurationMs);
        response.setRoot(root);
        response.setCriticalPathSpanIds(criticalPath);
        response.setGaps(gaps);

        if (bottleNeck != null) {
            response.setBottleneckSpanId(bottleNeck.getSpan().getSpanId());
            response.setBottleneckSelfTimeMs(bottleNeck.getSelfTimeMs());
        }

        return response;
    }
}
