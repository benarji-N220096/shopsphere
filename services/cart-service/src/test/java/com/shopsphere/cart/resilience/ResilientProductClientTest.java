package com.shopsphere.cart.resilience;

import com.shopsphere.cart.client.ProductClient;
import com.shopsphere.cart.dto.ProductDto;
import com.shopsphere.cart.exception.ProductNotFoundException;
import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResilientProductClientTest {

    @Mock
    private ProductClient feignClient;

    private ResilientProductClient resilientProductClient;

    @BeforeEach
    void setUp() {
        resilientProductClient = new ResilientProductClient(feignClient);
    }

    @Test
    void testGetProductById_Success() {
        UUID id = UUID.randomUUID();
        ProductDto dto = new ProductDto();
        dto.setId(id);
        dto.setName("Keyboard");
        dto.setPrice(BigDecimal.valueOf(49.99));
        when(feignClient.getProductById(id)).thenReturn(dto);

        ProductDto result = resilientProductClient.getProductById(id);
        assertNotNull(result);
        assertEquals("Keyboard", result.getName());
    }

    @Test
    void testGetProductByIdFallback_NotFoundRethrown() {
        UUID id = UUID.randomUUID();
        Request req = Request.create(Request.HttpMethod.GET, "/api/v1/products/" + id,
                Collections.emptyMap(), null, null, null);
        FeignException.NotFound notFound = new FeignException.NotFound("Not found", req, null, null);

        ProductNotFoundException ex = assertThrows(ProductNotFoundException.class, () ->
                resilientProductClient.getProductByIdFallback(id, notFound));
        assertTrue(ex.getMessage().contains("Product not found"));
    }

    @Test
    void testGetProductByIdFallback_CircuitOpen() {
        UUID id = UUID.randomUUID();
        CircuitBreaker cb = CircuitBreaker.of("cart-product", CircuitBreakerConfig.ofDefaults());
        cb.transitionToOpenState();
        CallNotPermittedException callNotPermitted = CallNotPermittedException.createCallNotPermittedException(cb);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                resilientProductClient.getProductByIdFallback(id, callNotPermitted));
        assertTrue(ex.getMessage().contains("circuit is OPEN"));
    }
}
