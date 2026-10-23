package com.shopsphere.order.client;

import com.shopsphere.order.config.FeignClientConfig;
import com.shopsphere.order.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "CART-SERVICE", contextId = "cartFeignClient", qualifiers = {"cartFeignClient"}, primary = false, configuration = FeignClientConfig.class)
public interface CartClient {

    @GetMapping("/api/v1/cart")
    CartResponse getCart(@RequestHeader("X-User-Id") UUID userId);

    @DeleteMapping("/api/v1/cart")
    void clearCart(@RequestHeader("X-User-Id") UUID userId);
}
