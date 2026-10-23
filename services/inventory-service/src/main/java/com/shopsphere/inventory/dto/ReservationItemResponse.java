package com.shopsphere.inventory.dto;

import java.util.UUID;

public class ReservationItemResponse {

    private UUID productId;
    private Integer quantity;

    public ReservationItemResponse() {
    }

    public ReservationItemResponse(UUID productId, Integer quantity) {
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
