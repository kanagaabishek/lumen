package com.lumen.server.ingestion;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcServer implements SmartLifecycle {

    private final TraceServiceImpl traceService;
    private Server server;
    private boolean running = false;

    @Value("${grpc.server.port:9090}")
    private int port;

    public GrpcServer(TraceServiceImpl traceService) {
        this.traceService = traceService;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder
                .forPort(port)
                .addService(traceService)
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();
            running = true;
            System.out.println("gRPC server started on port: " + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start gRPC server", e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            running = false;
            System.out.println("gRPC server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}