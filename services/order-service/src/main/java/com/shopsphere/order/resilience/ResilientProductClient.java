package com.shopsphere.order.resilience;

import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.dto.ProductDto;
import com.shopsphere.order.exception.ProductNotFoundException;
import com.shopsphere.order.exception.ProductUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Primary
@Component
public class ResilientProductClient implements ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientProductClient.class);

    private final ProductClient feignClient;

    public ResilientProductClient(@Qualifier("productFeignClient") ProductClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @CircuitBreaker(name = "order-product", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "order-product")
    public ProductDto getProductById(UUID id) {
        return feignClient.getProductById(id);
    }

    public ProductDto getProductByIdFallback(UUID id, Throwable t) {
        log.warn("Resilience fallback triggered for Product {} - cause: {}", id, t.getMessage());
        if (t instanceof FeignException.NotFound || (t instanceof FeignException fe && fe.status() == 404)) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }
        if (t instanceof ProductNotFoundException || t instanceof ProductUnavailableException) {
            throw (RuntimeException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-product circuit breaker prevented call for product {}", id);
            throw new ProductUnavailableException("Product Service circuit is OPEN. Service currently unavailable.");
        }
        throw new ProductUnavailableException("Product Service is currently unavailable: " + t.getMessage());
    }
}
