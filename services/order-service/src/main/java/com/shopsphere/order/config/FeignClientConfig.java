package com.shopsphere.order.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Value("${spring.application.name:order-service}")
    private String applicationName;

    @Value("${shopsphere.security.internal.secret:shopsphere-internal-service-secret-key-384}")
    private String internalSecret;

    @Bean
    public RequestInterceptor internalServiceRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Service", applicationName);
            requestTemplate.header("X-Internal-Secret", internalSecret);
        };
    }
}
