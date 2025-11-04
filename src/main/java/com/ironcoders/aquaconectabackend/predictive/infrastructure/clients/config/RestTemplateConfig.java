package com.ironcoders.aquaconectabackend.predictive.infrastructure.clients.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    /**
     * Creates a RestTemplate bean with configured timeouts.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))  // Connection timeout
            .setReadTimeout(Duration.ofSeconds(30))      // Read timeout (ML can be slow)
            .build();
    }
}
