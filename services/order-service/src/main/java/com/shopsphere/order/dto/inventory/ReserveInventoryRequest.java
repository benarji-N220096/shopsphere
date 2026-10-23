package com.shopsphere.order.dto.inventory;

import java.util.List;
import java.util.UUID;

public class ReserveInventoryRequest {

    private UUID orderId;
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
