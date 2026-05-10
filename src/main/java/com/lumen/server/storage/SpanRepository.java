package com.lumen.server.storage;

import com.lumen.server.domain.Span;
import com.lumen.server.domain.TraceSummary;

import java.util.List;

public interface SpanRepository {
    
    void save(Span span);

    List<Span> findByTraceId(String traceId);

    List<TraceSummary> findByServiceAndTimeWindow(
        String serviceName,
        long fromMs,
        long toMs
    );

    void saveService(String serviceName, long timestampMs);
    List<String> findAllServices();
}
