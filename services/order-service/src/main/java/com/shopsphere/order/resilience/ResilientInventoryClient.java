package com.shopsphere.order.resilience;

import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.inventory.ReserveInventoryRequest;
import com.shopsphere.order.exception.InsufficientStockException;
import com.shopsphere.order.exception.InventoryServiceUnavailableException;
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
public class ResilientInventoryClient implements InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientInventoryClient.class);

    private final InventoryClient feignClient;

    public ResilientInventoryClient(@Qualifier("inventoryFeignClient") InventoryClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @CircuitBreaker(name = "order-inventory", fallbackMethod = "getReservationFallback")
    @Retry(name = "order-inventory-idempotent")
    public ReservationResponse getReservationByOrderId(UUID orderId) {
        return feignClient.getReservationByOrderId(orderId);
    }

    public ReservationResponse getReservationFallback(UUID orderId, Throwable t) {
        if (t instanceof FeignException.NotFound || (t instanceof FeignException fe && fe.status() == 404)) {
            throw (FeignException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-inventory circuit breaker prevented getReservation for order {}", orderId);
            throw new InventoryServiceUnavailableException("Inventory Service circuit is OPEN. Service currently unavailable.");
        }
        throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable: " + t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "order-inventory", fallbackMethod = "reserveInventoryFallback")
    // NOTE: Intentionally NO @Retry on reserveInventory to prevent duplicate reservation calls!
    public ReservationResponse reserveInventory(ReserveInventoryRequest request) {
        return feignClient.reserveInventory(request);
    }

    public ReservationResponse reserveInventoryFallback(ReserveInventoryRequest request, Throwable t) {
        log.warn("Resilience fallback triggered for reserveInventory for order {} - cause: {}", request.getOrderId(), t.getMessage());
        if (t instanceof FeignException.Conflict || (t instanceof FeignException fe && fe.status() == 409)) {
            throw new InsufficientStockException("Insufficient stock available for requested products");
        }
        if (t instanceof InsufficientStockException || t instanceof InventoryServiceUnavailableException) {
            throw (RuntimeException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-inventory circuit breaker prevented reserveInventory for order {}", request.getOrderId());
            throw new InventoryServiceUnavailableException("Inventory Service circuit is OPEN. Service currently unavailable.");
        }
        throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable: " + t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "order-inventory", fallbackMethod = "confirmReservationFallback")
    @Retry(name = "order-inventory-idempotent")
    public ReservationResponse confirmReservation(UUID reservationId) {
        return feignClient.confirmReservation(reservationId);
    }

    public ReservationResponse confirmReservationFallback(UUID reservationId, Throwable t) {
        log.warn("Resilience fallback triggered for confirmReservation {} - cause: {}", reservationId, t.getMessage());
        if (t instanceof CallNotPermittedException) {
            throw new InventoryServiceUnavailableException("Inventory Service circuit is OPEN during confirmation.");
        }
        throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable: " + t.getMessage());
    }

    @Override
    @CircuitBreaker(name = "order-inventory", fallbackMethod = "cancelReservationFallback")
    @Retry(name = "order-inventory-idempotent")
    public ReservationResponse cancelReservation(UUID reservationId) {
        return feignClient.cancelReservation(reservationId);
    }

    public ReservationResponse cancelReservationFallback(UUID reservationId, Throwable t) {
        log.warn("Resilience fallback triggered for cancelReservation {} - cause: {}", reservationId, t.getMessage());
        if (t instanceof CallNotPermittedException) {
            throw new InventoryServiceUnavailableException("Inventory Service circuit is OPEN during cancellation.");
        }
        throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable: " + t.getMessage());
    }
}
