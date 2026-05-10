package com.lumen.server.api;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lumen.server.domain.TraceResponse;
import com.lumen.server.service.TraceQueryService;
import java.util.List;
import com.lumen.server.domain.TraceSummary;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
public class TraceQueryController {

    private final TraceQueryService traceQueryService;

    public TraceQueryController(TraceQueryService traceQueryService) {
        this.traceQueryService = traceQueryService;
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<TraceResponse> getTrace(@PathVariable String traceId) {
        TraceResponse response = traceQueryService.getTrace(traceId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/traces")
    public ResponseEntity<List<TraceSummary>> queryTraces(
        @RequestParam String service,
        @RequestParam long from,
        @RequestParam long to
    ) {

        // Max Range: 24 hours
        long maxRangeMs = 24 * 3_600_000L;
        if (to - from > maxRangeMs) {
            from = to - maxRangeMs;
        }
        
        List<TraceSummary> summaries = traceQueryService.listTraces(service, from, to);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> listServices() {
        List<String> services = traceQueryService.listServices();
        return ResponseEntity.ok(services);
    }
    
}
