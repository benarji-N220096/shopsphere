package com.shopsphere.order.dto.inventory;

import java.util.UUID;

public class ReservationItemRequest {

    private UUID productId;
    private Integer quantity;

    public ReservationItemRequest() {
    }

    public ReservationItemRequest(UUID productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
