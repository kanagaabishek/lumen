package com.lumen.server.ingestion;

import java.util.List;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import com.lumen.server.domain.Span;
import com.lumen.server.storage.SpanRepository;

@Component
public class SpanBatchWriter implements SmartLifecycle {

    private final SpanIngestionQueue ingestionQueue;
    private final SpanRepository repository;
    private volatile boolean running = false;
    private Thread writerThread;

    public SpanBatchWriter(SpanIngestionQueue ingestionQueue,
                           SpanRepository repository) {
        this.ingestionQueue = ingestionQueue;
        this.repository = repository;
    }

    @Override
    public void start() {
        running = true;
        writerThread = new Thread(this::writeLoop, "span-batch-writer");
        writerThread.setDaemon(true);
        writerThread.start();
        System.out.println("SpanBatchWriter started");
    }

    private void writeLoop() {
        while (running) {
            try {
                // drain up to 500 spans
                List<Span> batch = ingestionQueue.drain(500);

                if (!batch.isEmpty()) {
                    for (Span span : batch) {
                        repository.save(span);
                    }
                    System.out.println("Wrote batch of: " + batch.size());
                }
                Thread.sleep(100);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Batch write failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}