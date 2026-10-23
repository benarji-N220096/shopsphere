package com.shopsphere.inventory.controller;

import com.shopsphere.inventory.dto.ReservationItemRequest;
import com.shopsphere.inventory.dto.ReservationResponse;
import com.shopsphere.inventory.dto.ReserveInventoryRequest;
import com.shopsphere.inventory.service.InventoryReservationPersistenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inventory/reservations")
public class InventoryReservationController {

    private final InventoryReservationPersistenceService reservationPersistenceService;

    public InventoryReservationController(InventoryReservationPersistenceService reservationPersistenceService) {
        this.reservationPersistenceService = reservationPersistenceService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReservationResponse> getReservationByOrderId(@PathVariable UUID orderId) {
        ReservationResponse response = reservationPersistenceService.getReservationByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        // Aggregate items by productId prior to locking
        Map<UUID, Integer> aggregatedItems = request.getItems().stream()
                .collect(Collectors.toMap(
                        ReservationItemRequest::getProductId,
                        ReservationItemRequest::getQuantity,
                        Integer::sum
                ));

        ReservationResponse response = reservationPersistenceService.createReservationAtomic(
                request.getOrderId(),
                aggregatedItems
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable UUID reservationId) {
        ReservationResponse response = reservationPersistenceService.confirmReservationAtomic(reservationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable UUID reservationId) {
        ReservationResponse response = reservationPersistenceService.cancelReservationAtomic(reservationId);
        return ResponseEntity.ok(response);
    }
}
