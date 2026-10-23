package com.shopsphere.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ReserveInventoryRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotEmpty(message = "Reservation items must not be empty")
    @Valid
    private List<ReservationItemRequest> items;

    public ReserveInventoryRequest() {
    }

    public ReserveInventoryRequest(UUID orderId, List<ReservationItemRequest> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemRequest> items) {
        this.items = items;
    }
}
