package com.swiftpay.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swiftPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftPay — Transaction Gateway API")
                        .description("REST API for initiating and tracking real-time P2P payments. " +
                                "Supports idempotency, balance validation, and async Kafka-based processing.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SwiftPay Engineering")
                                .email("eng@swiftpay.io"))
                        .license(new License().name("MIT")));
    }
}
