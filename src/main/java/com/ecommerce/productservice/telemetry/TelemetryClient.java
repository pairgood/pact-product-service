package com.ecommerce.productservice.telemetry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class TelemetryClient {
    
    private final WebClient webClient;
    
    @Value("${telemetry.service.url:http://localhost:8086}")
    private String telemetryServiceUrl;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Autowired
    public TelemetryClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }
    
    public String startTrace(String operation, String httpMethod, String httpUrl, String userId) {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        Map<String, Object> eventData = createEventData();
        eventData.put("traceId", traceId);
        eventData.put("spanId", spanId);
        eventData.put("serviceName", serviceName);
        eventData.put("operation", operation);
        eventData.put("eventType", "SPAN");
        eventData.put("timestamp", LocalDateTime.now());
        eventData.put("status", "SUCCESS");
        eventData.put("httpMethod", httpMethod);
        eventData.put("httpUrl", httpUrl);
        eventData.put("userId", userId != null ? userId : "");
        sendTelemetryEvent(eventData);
        
        // Store in thread local for span context
        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(spanId);
        TraceContext.setStartTime(System.currentTimeMillis());
        
        return traceId;
    }
    
    public void finishTrace(String operation, int httpStatusCode, String errorMessage) {
        String traceId = TraceContext.getTraceId();
        String spanId = TraceContext.getSpanId();
        Long startTime = TraceContext.getStartTime();
        
        if (traceId == null || spanId == null) return;
        
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        Map<String, Object> eventData = createEventData();
        eventData.put("traceId", traceId);
        eventData.put("spanId", spanId);
        eventData.put("serviceName", serviceName);
        eventData.put("operation", operation + "_complete");
        eventData.put("eventType", "SPAN");
        eventData.put("timestamp", LocalDateTime.now());
        eventData.put("durationMs", duration);
        eventData.put("status", httpStatusCode >= 400 ? "ERROR" : "SUCCESS");
        eventData.put("httpStatusCode", httpStatusCode);
        eventData.put("errorMessage", errorMessage != null ? errorMessage : "");
        sendTelemetryEvent(eventData);
        
        // Clear trace context
        TraceContext.clear();
    }
    
    public void recordServiceCall(String targetService, String operation, String httpMethod, String url, long duration, int statusCode) {
        String traceId = TraceContext.getTraceId();
        String parentSpanId = TraceContext.getSpanId();
        
        if (traceId == null) return;
        
        String spanId = generateSpanId();
        
        Map<String, Object> eventData = createEventData();
        eventData.put("traceId", traceId);
        eventData.put("spanId", spanId);
        eventData.put("parentSpanId", parentSpanId);
        eventData.put("serviceName", serviceName);
        eventData.put("operation", targetService + "_" + operation);
        eventData.put("eventType", "SPAN");
        eventData.put("timestamp", LocalDateTime.now());
        eventData.put("durationMs", duration);
        eventData.put("status", statusCode >= 400 ? "ERROR" : "SUCCESS");
        eventData.put("httpMethod", httpMethod);
        eventData.put("httpUrl", url);
        eventData.put("httpStatusCode", statusCode);
        eventData.put("metadata", "Outbound call to " + targetService);
        sendTelemetryEvent(eventData);
    }
    
    public void logEvent(String message, String level) {
        String traceId = TraceContext.getTraceId();
        String spanId = TraceContext.getSpanId();
        
        if (traceId == null) return;
        
        Map<String, Object> eventData = createEventData();
        eventData.put("traceId", traceId);
        eventData.put("spanId", spanId);
        eventData.put("serviceName", serviceName);
        eventData.put("operation", "log_" + level.toLowerCase());
        eventData.put("eventType", "LOG");
        eventData.put("timestamp", LocalDateTime.now());
        eventData.put("status", "SUCCESS");
        eventData.put("metadata", message);
        sendTelemetryEvent(eventData);
    }
    
    private void sendTelemetryEvent(Map<String, Object> eventData) {
        try {
            webClient.post()
                .uri(telemetryServiceUrl + "/api/telemetry/events")
                .bodyValue(eventData)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                    result -> {},
                    error -> System.err.println("Failed to send telemetry: " + error.getMessage())
                );
        } catch (Exception e) {
            // Silently fail - telemetry should not affect application functionality
        }
    }
    
    private Map<String, Object> createEventData() {
        return new java.util.HashMap<>();
    }
    
    private String generateTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateSpanId() {
        return "span_" + Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
    
    public static class TraceContext {
        private static final ThreadLocal<String> traceId = new ThreadLocal<>();
        private static final ThreadLocal<String> spanId = new ThreadLocal<>();
        private static final ThreadLocal<Long> startTime = new ThreadLocal<>();
        
        public static void setTraceId(String id) { traceId.set(id); }
        public static String getTraceId() { return traceId.get(); }
        
        public static void setSpanId(String id) { spanId.set(id); }
        public static String getSpanId() { return spanId.get(); }
        
        public static void setStartTime(Long time) { startTime.set(time); }
        public static Long getStartTime() { return startTime.get(); }
        
        public static void clear() {
            traceId.remove();
            spanId.remove();
            startTime.remove();
        }
        
        public static void propagate(String trace, String span) {
            traceId.set(trace);
            spanId.set(span);
        }
    }
}