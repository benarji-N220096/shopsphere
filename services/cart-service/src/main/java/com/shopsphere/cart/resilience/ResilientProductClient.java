package com.shopsphere.cart.resilience;

import com.shopsphere.cart.client.ProductClient;
import com.shopsphere.cart.dto.ProductDto;
import com.shopsphere.cart.exception.ProductNotFoundException;
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

    public ResilientProductClient(@Qualifier("cartProductFeignClient") ProductClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @CircuitBreaker(name = "cart-product", fallbackMethod = "getProductByIdFallback")
    @Retry(name = "cart-product")
    public ProductDto getProductById(UUID id) {
        return feignClient.getProductById(id);
    }

    public ProductDto getProductByIdFallback(UUID id, Throwable t) {
        log.warn("Cart-Product resilience fallback for Product {} - cause: {}", id, t.getMessage());
        if (t instanceof FeignException.NotFound || (t instanceof FeignException fe && fe.status() == 404)) {
            throw new ProductNotFoundException("Product not found with ID: " + id);
        }
        if (t instanceof ProductNotFoundException) {
            throw (ProductNotFoundException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: cart-product circuit breaker prevented getProduct for product {}", id);
            throw new RuntimeException("Product Service circuit is OPEN. Service currently unavailable.");
        }
        throw new RuntimeException("Product Service is currently unavailable: " + t.getMessage(), t);
    }
}
