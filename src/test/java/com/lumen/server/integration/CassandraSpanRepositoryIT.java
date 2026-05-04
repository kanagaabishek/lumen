package com.lumen.server.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.lumen.server.storage.CassandraSpanRepository;
import com.datastax.oss.driver.api.core.CqlSession;
import com.lumen.server.domain.Span;

public class CassandraSpanRepositoryIT {

    private CqlSession session;
    private CassandraSpanRepository repository;

    @BeforeEach
    public void setup() {
        session = CqlSession.builder()
            .withLocalDatacenter("datacenter1")
            .build();
        repository = new CassandraSpanRepository(session);
    }

    @Test
    public void testSaveAndFindByTraceId() {
        Span span = new Span();
        span.setTraceId("test-trace-id");
        span.setSpanId("test-span-id");
        span.setParentSpan(null);
        span.setServiceName("test-service");
        span.setOperationName("test-operation");
        long startNano = System.nanoTime();
        span.setStartTimeNano(startNano);
        span.setEndTimeNano(startNano + 1_000_000);
        span.setHasError(false);

        repository.save(span);

        var spans = repository.findByTraceId("test-trace-id");
        assertNotNull(spans);
        assertEquals(1, spans.size());
        assertEquals("test-span-id", spans.get(0).getSpanId());
        assertEquals("test-service", spans.get(0).getServiceName());
    }
}
