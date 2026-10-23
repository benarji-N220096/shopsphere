package com.shopsphere.order.resilience;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.dto.CartResponse;
import com.shopsphere.order.exception.CartServiceUnavailableException;
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
public class ResilientCartClient implements CartClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientCartClient.class);

    private final CartClient feignClient;

    public ResilientCartClient(@Qualifier("cartFeignClient") CartClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @CircuitBreaker(name = "order-cart", fallbackMethod = "getCartFallback")
    @Retry(name = "order-cart")
    public CartResponse getCart(UUID userId) {
        return feignClient.getCart(userId);
    }

    public CartResponse getCartFallback(UUID userId, Throwable t) {
        log.warn("Resilience fallback triggered for getCart for user {} - cause: {}", userId, t.getMessage());
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-cart circuit breaker prevented getCart for user {}", userId);
            throw new CartServiceUnavailableException("Cart Service circuit is OPEN. Service currently unavailable.");
        }
        if (t instanceof CartServiceUnavailableException) {
            throw (CartServiceUnavailableException) t;
        }
        if (t instanceof RuntimeException re && !(t instanceof FeignException)) {
            throw re;
        }
        throw new CartServiceUnavailableException("Cart Service is currently unavailable: " + t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "order-cart", fallbackMethod = "clearCartFallback")
    public void clearCart(UUID userId) {
        feignClient.clearCart(userId);
    }

    public void clearCartFallback(UUID userId, Throwable t) {
        log.warn("Resilience fallback triggered for clearCart for user {} - cause: {}. Preserving confirmed order.", userId, t.getMessage());
        // Phase 2/3 Rule: Cart clear failure does NOT revert confirmed order.
    }
}
