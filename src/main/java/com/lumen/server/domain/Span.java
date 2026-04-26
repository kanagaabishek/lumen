package com.lumen.server.domain;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
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

    // Setters and Getters
    public String getSpanId(){
        return spanId;
    }

    public void setSpanId(String id){
        this.spanId = id;
    }

    public String getTraceId(){
        return traceId;
    }

    public void setTraceId(String traceId){
        this.traceId = traceId;
    }

    public String getParentSpanId(){ 
        return parentSpanId;
    }

    public void setParentSpan(String parentSpanId){
        this.parentSpanId = parentSpanId;
    }

    public String getServiceName(){
        return serviceName;
    }

    public void setServiceName(String serviceName){
        this.serviceName = serviceName;
    }

    public String getOperationName(){
        return operationName;
    }

    public void setOperationName(String operationName){
        this.operationName = operationName;
    }

    public long getstartTimeNano(){
        return startTimeNano;
    }

    public void setStartTimeNano(long startTimeNano){
        this.startTimeNano = startTimeNano;
    }

    public long getEndTimeNano(){
        return endTimeNano;
    }

    public void setEndTimeNano(long endTimeNano){
        this.endTimeNano = endTimeNano;
    }

    public Map<String, String> getAttributes(){
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes){
        this.attributes = attributes;
    }

    public boolean isHasError(){
        return hasError;
    }

    public void setHasError(boolean hasError){
        this.hasError = hasError;
    }

    public long getDurationMs() {
        return (endTimeNano - startTimeNano) / 1_000_000;
    }
}