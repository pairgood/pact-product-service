package com.ecommerce.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TracingConfig {

    /**
     * WebClient bean using Spring Boot's auto-configured, OTel-instrumented builder.
     * WebClient.Builder in Spring Boot 3.x auto-configures OTel propagation
     * when micrometer-tracing-bridge-otel is on the classpath.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}
