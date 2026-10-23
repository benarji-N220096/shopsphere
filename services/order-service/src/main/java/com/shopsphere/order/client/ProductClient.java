package com.shopsphere.order.client;

import com.shopsphere.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.shopsphere.order.config.FeignClientConfig;

import java.util.UUID;

@FeignClient(name = "PRODUCT-SERVICE", contextId = "productFeignClient", qualifiers = {"productFeignClient"}, primary = false, configuration = FeignClientConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    ProductDto getProductById(@PathVariable("id") UUID id);
}
