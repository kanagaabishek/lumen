package com.lumen.server.ingestion;

import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

import com.lumen.server.domain.Span;
import com.lumen.server.storage.SpanRepository;

import io.opentelemetry.proto.collector.trace.v1.ExportTracePartialSuccess;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.grpc.stub.StreamObserver;

public class TraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase{
    private final SpanRepository repository;
    
    public TraceServiceImpl(SpanRepository repository) {
        this.repository = repository;
    }

    private Span convertToSpan(
        io.opentelemetry.proto.trace.v1.Span protoSpan,
        String serviceName) {
        String traceId = HexFormat.of().formatHex(protoSpan.getTraceId().toByteArray()); 
        String spanId = HexFormat.of().formatHex(protoSpan.getSpanId().toByteArray());
        String parentSpanId = protoSpan.getParentSpanId().isEmpty() ? null : HexFormat.of().formatHex(protoSpan.getParentSpanId().toByteArray());
        long startTime = protoSpan.getStartTimeUnixNano();
        long endTime = protoSpan.getEndTimeUnixNano();
        
        Map<String, String> attributes = protoSpan.getAttributesList().stream()
            .collect(Collectors.toMap(
                kv -> kv.getKey(),
                kv -> kv.getValue().getStringValue()
            ));

        boolean hasError = protoSpan.getStatus().getCode() == io.opentelemetry.proto.trace.v1.Status.StatusCode.STATUS_CODE_ERROR;

        Span span = new Span(
            spanId,
            traceId,
            parentSpanId,
            serviceName,
            protoSpan.getName(),
            startTime,
            endTime,
            attributes,
            hasError
        );
        
        return span;
    }

    private String extractServiceName(ResourceSpans resourceSpans) {
        // Extract service name from resource attributes
        return resourceSpans.getResource()
            .getAttributesList()
            .stream()
            .filter(attr -> attr.getKey().equals("service.name"))
            .findFirst()
            .map(attr -> attr.getValue().getStringValue())
            .orElse("unknown");
    }


    @Override
    public void export(ExportTraceServiceRequest request,
                   StreamObserver<ExportTraceServiceResponse> responseObserver) {
    
        int failedCount = 0;
        for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
            String serviceName = extractServiceName(resourceSpans);
            
            for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
                for (var protoSpan : scopeSpans.getSpansList()) {
                    try {
                        Span span = convertToSpan(protoSpan, serviceName);
                        repository.save(span);
                    } catch (Exception e) {
                        failedCount++;
                    }
                }
            }
        }
        
        ExportTraceServiceResponse response = ExportTraceServiceResponse
            .newBuilder()
            .setPartialSuccess(
                ExportTracePartialSuccess.newBuilder().setRejectedSpans(failedCount)
            )
            .build();
        
            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }


}
