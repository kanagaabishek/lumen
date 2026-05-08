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
        Span span = new Span("test-trace-id", "test-span-id", null, "test-service", "test-operation", System.nanoTime(), System.nanoTime() + 1_000_000,null,false);

        repository.save(span);

        var spans = repository.findByTraceId("test-trace-id");
        assertNotNull(spans);
        assertEquals(1, spans.size());
        assertEquals("test-span-id", spans.get(0).getSpanId());
        assertEquals("test-service", spans.get(0).getServiceName());
    }
}
