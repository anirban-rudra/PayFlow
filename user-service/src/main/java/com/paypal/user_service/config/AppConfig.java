package com.paypal.user_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public RequestInterceptor internalApiKeyInterceptor(
            @Value("${app.security.internal-api-key}") String internalApiKey) {
        return template -> template.header("X-Internal-Api-Key", internalApiKey);
    }
}
