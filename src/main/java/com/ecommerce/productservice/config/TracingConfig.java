package com.ecommerce.productservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TracingConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // WebClient.Builder in Spring Boot 3.x auto-configures OTel propagation
        // when the micrometer-tracing-bridge-otel dependency is on the classpath.
        // Just inject the builder — do not construct WebClient.create() directly.
        return builder.build();
    }
}
