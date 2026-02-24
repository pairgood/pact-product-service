package com.ecommerce.productservice.telemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TelemetryClientTest {

    private TelemetryClient telemetryClient;

    @BeforeEach
    void setUp() {
        telemetryClient = new TelemetryClient(WebClient.builder());
        ReflectionTestUtils.setField(telemetryClient, "telemetryServiceUrl", "http://localhost:8086");
        ReflectionTestUtils.setField(telemetryClient, "serviceName", "product-service");
    }

    @Test
    void constructor_ShouldCreateTelemetryClient() {
        // When
        TelemetryClient client = new TelemetryClient(WebClient.builder());

        // Then
        assertThat(client).isNotNull();
    }

    @Test
    void startTrace_ShouldReturnTraceId() {
        // When
        String traceId = telemetryClient.startTrace("test_operation", "GET", "/api/test", "user123");

        // Then
        assertThat(traceId).isNotNull();
        assertThat(traceId).startsWith("trace_");
        assertThat(traceId).hasSize(38); // "trace_" + 32 hex characters
    }

    @Test
    void startTrace_WithNullUserId_ShouldReturnTraceId() {
        // When
        String traceId = telemetryClient.startTrace("test_operation", "GET", "/api/test", null);

        // Then
        assertThat(traceId).isNotNull();
        assertThat(traceId).startsWith("trace_");
    }

    @Test
    void startTrace_ShouldSetTraceContext() {
        // When
        String traceId = telemetryClient.startTrace("test_operation", "GET", "/api/test", "user123");

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNotNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNotNull();
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void finishTrace_WithValidContext_ShouldNotThrowException() {
        // Given
        String traceId = telemetryClient.startTrace("test_operation", "GET", "/api/test", "user123");

        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.finishTrace("test_operation", 200, null)
        );
        
        // Verify context is cleared
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNull();
    }

    @Test
    void finishTrace_WithErrorStatus_ShouldNotThrowException() {
        // Given
        String traceId = telemetryClient.startTrace("test_operation", "GET", "/api/test", "user123");

        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.finishTrace("test_operation", 500, "Internal Server Error")
        );
    }

    @Test
    void finishTrace_WithoutActiveTrace_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.finishTrace("test_operation", 200, null)
        );
    }

    @Test
    void recordServiceCall_WithActiveTrace_ShouldNotThrowException() {
        // Given
        telemetryClient.startTrace("parent_operation", "GET", "/api/test", "user123");

        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.recordServiceCall("user-service", "get_user", "GET", "/api/users/1", 150, 200)
        );
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void recordServiceCall_WithoutActiveTrace_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.recordServiceCall("user-service", "get_user", "GET", "/api/users/1", 150, 200)
        );
    }

    @Test
    void logEvent_WithActiveTrace_ShouldNotThrowException() {
        // Given
        telemetryClient.startTrace("test_operation", "GET", "/api/test", "user123");

        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.logEvent("Test log message", "INFO")
        );
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void logEvent_WithoutActiveTrace_ShouldNotThrowException() {
        // When & Then
        assertDoesNotThrow(() -> 
            telemetryClient.logEvent("Test log message", "ERROR")
        );
    }

    @Test
    void traceContext_SetAndGetTraceId_ShouldWork() {
        // Given
        String testTraceId = "test-trace-123";

        // When
        TelemetryClient.TraceContext.setTraceId(testTraceId);

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(testTraceId);
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void traceContext_SetAndGetSpanId_ShouldWork() {
        // Given
        String testSpanId = "test-span-456";

        // When
        TelemetryClient.TraceContext.setSpanId(testSpanId);

        // Then
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo(testSpanId);
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void traceContext_SetAndGetStartTime_ShouldWork() {
        // Given
        Long testStartTime = System.currentTimeMillis();

        // When
        TelemetryClient.TraceContext.setStartTime(testStartTime);

        // Then
        assertThat(TelemetryClient.TraceContext.getStartTime()).isEqualTo(testStartTime);
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void traceContext_Clear_ShouldRemoveAllValues() {
        // Given
        TelemetryClient.TraceContext.setTraceId("test-trace");
        TelemetryClient.TraceContext.setSpanId("test-span");
        TelemetryClient.TraceContext.setStartTime(System.currentTimeMillis());

        // When
        TelemetryClient.TraceContext.clear();

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
        assertThat(TelemetryClient.TraceContext.getSpanId()).isNull();
        assertThat(TelemetryClient.TraceContext.getStartTime()).isNull();
    }

    @Test
    void traceContext_Propagate_ShouldSetValues() {
        // Given
        String testTraceId = "propagated-trace";
        String testSpanId = "propagated-span";

        // When
        TelemetryClient.TraceContext.propagate(testTraceId, testSpanId);

        // Then
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(testTraceId);
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo(testSpanId);
        
        // Clean up
        TelemetryClient.TraceContext.clear();
    }

    @Test
    void startTrace_ShouldGenerateUniqueTraceIds() {
        // When
        String traceId1 = telemetryClient.startTrace("operation1", "GET", "/api/test1", "user1");
        TelemetryClient.TraceContext.clear();
        String traceId2 = telemetryClient.startTrace("operation2", "GET", "/api/test2", "user2");
        TelemetryClient.TraceContext.clear();

        // Then
        assertThat(traceId1).isNotEqualTo(traceId2);
        assertThat(traceId1).startsWith("trace_");
        assertThat(traceId2).startsWith("trace_");
    }

    @Test
    void telemetryClient_ShouldHandleNetworkExceptionsGracefully() {
        // Given - TelemetryClient with invalid URL
        TelemetryClient clientWithBadUrl = new TelemetryClient(WebClient.builder());
        ReflectionTestUtils.setField(clientWithBadUrl, "telemetryServiceUrl", "http://invalid-url:9999");
        ReflectionTestUtils.setField(clientWithBadUrl, "serviceName", "product-service");

        // When & Then - Should not throw exceptions even with network issues
        assertDoesNotThrow(() -> {
            String traceId = clientWithBadUrl.startTrace("test_operation", "GET", "/api/test", "user123");
            clientWithBadUrl.finishTrace("test_operation", 200, null);
            clientWithBadUrl.recordServiceCall("user-service", "get_user", "GET", "/api/users/1", 150, 200);
            clientWithBadUrl.logEvent("Test message", "INFO");
        });
    }

    @Test
    void telemetryClient_MultipleOperations_ShouldMaintainCorrectContext() {
        // Given
        String traceId = telemetryClient.startTrace("operation1", "GET", "/api/test", "user123");

        // When
        String contextTraceId = TelemetryClient.TraceContext.getTraceId();
        String contextSpanId = TelemetryClient.TraceContext.getSpanId();
        Long contextStartTime = TelemetryClient.TraceContext.getStartTime();

        // Then
        assertThat(contextTraceId).isEqualTo(traceId);
        assertThat(contextSpanId).isNotNull();
        assertThat(contextStartTime).isNotNull();

        // When - Record service call and log event within same trace
        assertDoesNotThrow(() -> {
            telemetryClient.recordServiceCall("user-service", "get_user", "GET", "/api/users/1", 100, 200);
            telemetryClient.logEvent("Operation completed", "INFO");
        });

        // Then - Context should still be the same
        assertThat(TelemetryClient.TraceContext.getTraceId()).isEqualTo(traceId);
        assertThat(TelemetryClient.TraceContext.getSpanId()).isEqualTo(contextSpanId);

        // Clean up
        telemetryClient.finishTrace("operation1", 200, null);
        assertThat(TelemetryClient.TraceContext.getTraceId()).isNull();
    }
}