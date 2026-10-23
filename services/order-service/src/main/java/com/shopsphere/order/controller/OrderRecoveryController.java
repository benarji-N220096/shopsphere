package com.shopsphere.order.controller;

import com.shopsphere.order.dto.BatchRecoveryResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.service.OrderRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller for manual and batch order recovery operations.
 * NOTE: These endpoints are intended strictly for internal/admin use.
 * They are not exposed through the public API Gateway.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderRecoveryController {

    private final OrderRecoveryService orderRecoveryService;

    public OrderRecoveryController(OrderRecoveryService orderRecoveryService) {
        this.orderRecoveryService = orderRecoveryService;
    }

    /**
     * Manual recovery of a single order by orderId.
     */
    @PostMapping("/{orderId}/recover")
    public ResponseEntity<OrderResponse> recoverOrder(@PathVariable UUID orderId) {
        OrderResponse response = orderRecoveryService.recoverOrder(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Batch recovery of all eligible stale PENDING orders.
     */
    @PostMapping("/recover-pending")
    public ResponseEntity<BatchRecoveryResponse> recoverPendingOrders() {
        BatchRecoveryResponse response = orderRecoveryService.recoverStalePendingOrders();
        return ResponseEntity.ok(response);
    }
}
