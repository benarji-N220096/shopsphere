package com.shopsphere.cart.client;

import com.shopsphere.cart.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "PRODUCT-SERVICE", contextId = "cartProductFeignClient", qualifiers = {"cartProductFeignClient"}, primary = false, configuration = com.shopsphere.cart.config.FeignClientConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductDto getProductById(@PathVariable("id") UUID id);
}
