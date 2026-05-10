package com.lumen.server.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

import com.lumen.server.domain.Span;

@Component
public class SpanIngestionQueue {

    private final BlockingQueue<Span> queue;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong ingestedCount = new AtomicLong(0);

    public SpanIngestionQueue() {
        this.queue = new LinkedBlockingQueue<>(10_000); // fixed capacity
    }

    public boolean offer(Span span) {
        if(queue.size() >= 10_000) {
            droppedCount.incrementAndGet();
            return false;
        }
        boolean result = queue.offer(span);
        if (result) {
            ingestedCount.incrementAndGet();
        }
        return result;
    }

    public List<Span> drain(int maxBatch) {
        List<Span> batch = new ArrayList<>(maxBatch);
        queue.drainTo(batch, maxBatch);
        return batch;
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }

    public int getQueueDepth() {
        return queue.size();
    }

    public long getIngestedCount() {
        return ingestedCount.get();
    }
}
