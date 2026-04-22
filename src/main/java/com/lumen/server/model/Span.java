package com.lumen.server.model;

import java.util.Map;

public class Span{
    private String spanId;
    private String traceId;
    private String parentSpanId;
    private String serviceName;
    private String operationName;
    private long startTimeNano;
    private long endTimeNano;
    private Map<String, String> attributes;
    private boolean hasError;

}