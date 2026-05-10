package com.lumen.server.unit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.lumen.server.domain.Span;
import com.lumen.server.domain.TraceSummary;
import com.lumen.server.storage.SpanRepository;

import java.util.List;

@Configuration
public class TestConfiguration {

    @Bean
    public SpanRepository spanRepository() {
        return new SpanRepository() {
            @Override
            public void save(Span span) {
            }

            @Override
            public List<Span> findByTraceId(String traceId) {
                return List.of();
            }

            @Override
            public List<TraceSummary> findByServiceAndTimeWindow(String serviceName, long fromMs, long toMs) {
                return List.of();
            }

            @Override
            public void saveService(String serviceName, long timestampMs) {
            }

            @Override
            public List<String> findAllServices() {
                return List.of();
            }
        };
    }
}
