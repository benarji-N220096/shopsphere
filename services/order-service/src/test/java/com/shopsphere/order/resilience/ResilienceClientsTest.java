package com.shopsphere.order.resilience;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.dto.CartResponse;
import com.shopsphere.order.dto.ProductDto;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.inventory.ReserveInventoryRequest;
import com.shopsphere.order.dto.payment.PaymentRequest;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.exception.CartServiceUnavailableException;
import com.shopsphere.order.exception.InsufficientStockException;
import com.shopsphere.order.exception.InventoryServiceUnavailableException;
import com.shopsphere.order.exception.PaymentServiceUnavailableException;
import com.shopsphere.order.exception.ProductNotFoundException;
import com.shopsphere.order.exception.ProductUnavailableException;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilienceClientsTest {

    @Mock
    private ProductClient productFeignClient;

    @Mock
    private CartClient cartFeignClient;

    @Mock
    private InventoryClient inventoryFeignClient;

    @Mock
    private PaymentClient paymentFeignClient;

    private ResilientProductClient resilientProductClient;
    private ResilientCartClient resilientCartClient;
    private ResilientInventoryClient resilientInventoryClient;
    private ResilientPaymentClient resilientPaymentClient;

    @BeforeEach
    void setUp() {
        resilientProductClient = new ResilientProductClient(productFeignClient);
        resilientCartClient = new ResilientCartClient(cartFeignClient);
        resilientInventoryClient = new ResilientInventoryClient(inventoryFeignClient);
        resilientPaymentClient = new ResilientPaymentClient(paymentFeignClient);
    }

    // ==========================================
    // 1. PRODUCT RESILIENCE TESTS
    // ==========================================

    @Test
    void testProductClient_Success() {
        UUID productId = UUID.randomUUID();
        ProductDto dto = new ProductDto();
        dto.setId(productId);
        dto.setName("Wireless Mouse");
        dto.setPrice(BigDecimal.valueOf(25.00));
        when(productFeignClient.getProductById(productId)).thenReturn(dto);

        ProductDto result = resilientProductClient.getProductById(productId);
        assertNotNull(result);
        assertEquals("Wireless Mouse", result.getName());
    }

    @Test
    void testProductClient_NotFoundRethrownWithoutRetry() {
        UUID productId = UUID.randomUUID();
        Request feignRequest = Request.create(Request.HttpMethod.GET, "/api/v1/products/" + productId,
                Collections.emptyMap(), null, null, null);
        FeignException.NotFound notFound = new FeignException.NotFound("Not Found", feignRequest, null, null);

        Throwable thrown = assertThrows(ProductNotFoundException.class, () ->
                resilientProductClient.getProductByIdFallback(productId, notFound));
        assertTrue(thrown.getMessage().contains("Product not found"));
    }

    @Test
    void testProductClient_CircuitOpenFallback() {
        UUID productId = UUID.randomUUID();
        CircuitBreaker cb = CircuitBreaker.of("order-product", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(cb);

        ProductUnavailableException ex = assertThrows(ProductUnavailableException.class, () ->
                resilientProductClient.getProductByIdFallback(productId, callNotPermitted));
        assertTrue(ex.getMessage().contains("circuit is OPEN"));
    }

    // ==========================================
    // 2. CART RESILIENCE TESTS
    // ==========================================

    @Test
    void testCartClient_GetCartSuccess() {
        UUID userId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        when(cartFeignClient.getCart(userId)).thenReturn(cart);

        CartResponse result = resilientCartClient.getCart(userId);
        assertNotNull(result);
    }

    @Test
    void testCartClient_GetCartFallbackOnCircuitOpen() {
        UUID userId = UUID.randomUUID();
        CircuitBreaker cb = CircuitBreaker.of("order-cart", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(cb);

        CartServiceUnavailableException ex = assertThrows(CartServiceUnavailableException.class, () ->
                resilientCartClient.getCartFallback(userId, callNotPermitted));
        assertTrue(ex.getMessage().contains("circuit is OPEN"));
    }

    @Test
    void testCartClient_ClearCartFallbackDoesNotThrow() {
        UUID userId = UUID.randomUUID();
        // Clear cart fallback must silently log and NOT throw exception to protect confirmed order
        assertDoesNotThrow(() ->
                resilientCartClient.clearCartFallback(userId, new ConnectException("Connection refused")));
    }

    // ==========================================
    // 3. INVENTORY RESILIENCE TESTS
    // ==========================================

    @Test
    void testInventoryClient_ReserveSuccess() {
        ReserveInventoryRequest req = new ReserveInventoryRequest(UUID.randomUUID(), Collections.emptyList());
        ReservationResponse res = new ReservationResponse();
        res.setStatus("RESERVED");
        when(inventoryFeignClient.reserveInventory(req)).thenReturn(res);

        ReservationResponse result = resilientInventoryClient.reserveInventory(req);
        assertEquals("RESERVED", result.getStatus());
    }

    @Test
    void testInventoryClient_ReserveStockConflictRethrown() {
        ReserveInventoryRequest req = new ReserveInventoryRequest(UUID.randomUUID(), Collections.emptyList());
        Request feignRequest = Request.create(Request.HttpMethod.POST, "/api/v1/inventory/reservations",
                Collections.emptyMap(), null, null, null);
        FeignException.Conflict conflict = new FeignException.Conflict("Insufficient stock", feignRequest, null, null);

        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () ->
                resilientInventoryClient.reserveInventoryFallback(req, conflict));
        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }

    @Test
    void testInventoryClient_CircuitOpenFallback() {
        ReserveInventoryRequest req = new ReserveInventoryRequest(UUID.randomUUID(), Collections.emptyList());
        CircuitBreaker cb = CircuitBreaker.of("order-inventory", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(cb);

        InventoryServiceUnavailableException ex = assertThrows(InventoryServiceUnavailableException.class, () ->
                resilientInventoryClient.reserveInventoryFallback(req, callNotPermitted));
        assertTrue(ex.getMessage().contains("circuit is OPEN"));
    }

    // ==========================================
    // 4. PAYMENT RESILIENCE & IDEMPOTENCY TESTS
    // ==========================================

    @Test
    void testPaymentClient_Success() {
        PaymentRequest req = new PaymentRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "USD", "CREDIT_CARD", "key-1", "SUCCESS");
        PaymentResponse res = new PaymentResponse();
        res.setStatus("SUCCESS");
        when(paymentFeignClient.processPayment(req)).thenReturn(res);

        PaymentResponse result = resilientPaymentClient.processPayment(req);
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void testPaymentClient_FallbackThrowsPaymentServiceUnavailableException() {
        UUID orderId = UUID.randomUUID();
        PaymentRequest req = new PaymentRequest(orderId, UUID.randomUUID(), BigDecimal.TEN, "USD", "CREDIT_CARD", "key-1", "SUCCESS");

        PaymentServiceUnavailableException ex = assertThrows(PaymentServiceUnavailableException.class, () ->
                resilientPaymentClient.processPaymentFallback(req, new ConnectException("Connection refused")));
        assertTrue(ex.getMessage().contains("Payment Service is currently unavailable for order " + orderId));
    }

    @Test
    void testPaymentClient_CircuitOpenThrowsPaymentServiceUnavailableException() {
        UUID orderId = UUID.randomUUID();
        PaymentRequest req = new PaymentRequest(orderId, UUID.randomUUID(), BigDecimal.TEN, "USD", "CREDIT_CARD", "key-1", "SUCCESS");
        CircuitBreaker cb = CircuitBreaker.of("order-payment", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(cb);

        PaymentServiceUnavailableException ex = assertThrows(PaymentServiceUnavailableException.class, () ->
                resilientPaymentClient.processPaymentFallback(req, callNotPermitted));
        assertTrue(ex.getMessage().contains("circuit is OPEN"));
    }

    // ==========================================
    // 5. CIRCUIT BREAKER STATE TRANSITION TEST
    // ==========================================

    @Test
    void testCircuitBreaker_StateTransitions_ClosedToOpenToHalfOpenToClosed() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = registry.circuitBreaker("test-breaker");

        // 1. Initially CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        // 2. Cause failures to exceed threshold
        cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("fail 1"));
        cb.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, new RuntimeException("fail 2"));

        // 3. State is now OPEN
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.tryAcquirePermission(), "Calls must not be permitted in OPEN state");

        // 4. Wait for waitDurationInOpenState to transition to HALF_OPEN
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}

        // In Resilience4J, acquiring permission after waitDuration moves it to HALF_OPEN
        assertTrue(cb.tryAcquirePermission());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());

        // 5. Successful test calls in HALF_OPEN close the circuit
        cb.onSuccess(0, java.util.concurrent.TimeUnit.MILLISECONDS);
        cb.tryAcquirePermission();
        cb.onSuccess(0, java.util.concurrent.TimeUnit.MILLISECONDS);

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }
}
