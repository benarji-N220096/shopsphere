package com.shopsphere.order.client;

import com.shopsphere.order.dto.payment.PaymentRequest;
import com.shopsphere.order.dto.payment.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.shopsphere.order.config.FeignClientConfig;

import java.util.UUID;

@FeignClient(name = "PAYMENT-SERVICE", contextId = "paymentFeignClient", qualifiers = {"paymentFeignClient"}, primary = false, configuration = FeignClientConfig.class)
public interface PaymentClient {

    @PostMapping("/api/v1/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);

    @GetMapping("/api/v1/payments/order/{orderId}")
    PaymentResponse getPaymentByOrderId(@PathVariable("orderId") UUID orderId);
}
