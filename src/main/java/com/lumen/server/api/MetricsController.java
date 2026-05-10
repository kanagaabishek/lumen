package com.lumen.server.api;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lumen.server.ingestion.SpanIngestionQueue;

@RestController
@RequestMapping("/api")
public class MetricsController {

    private final SpanIngestionQueue ingestionQueue;

    public MetricsController(SpanIngestionQueue ingestionQueue) {
        this.ingestionQueue = ingestionQueue;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("queue_depth", ingestionQueue.getQueueDepth());
        metrics.put("spans_dropped_total", ingestionQueue.getDroppedCount());
        metrics.put("spans_ingested_total", ingestionQueue.getIngestedCount());
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("grpc_port", "9090");
        status.put("http_port", "8080");
        return ResponseEntity.ok(status);
    }
}
