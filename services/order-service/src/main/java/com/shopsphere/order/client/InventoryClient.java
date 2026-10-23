package com.shopsphere.order.client;

import com.shopsphere.order.config.FeignClientConfig;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.inventory.ReserveInventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "INVENTORY-SERVICE", contextId = "inventoryFeignClient", qualifiers = {"inventoryFeignClient"}, primary = false, configuration = FeignClientConfig.class)
public interface InventoryClient {

    @GetMapping("/api/v1/inventory/reservations/order/{orderId}")
    ReservationResponse getReservationByOrderId(@PathVariable("orderId") UUID orderId);

    @PostMapping("/api/v1/inventory/reservations")
    ReservationResponse reserveInventory(@RequestBody ReserveInventoryRequest request);

    @PostMapping("/api/v1/inventory/reservations/{reservationId}/confirm")
    ReservationResponse confirmReservation(@PathVariable("reservationId") UUID reservationId);

    @PostMapping("/api/v1/inventory/reservations/{reservationId}/cancel")
    ReservationResponse cancelReservation(@PathVariable("reservationId") UUID reservationId);
}
