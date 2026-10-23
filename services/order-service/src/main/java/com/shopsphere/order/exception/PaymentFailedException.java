package com.shopsphere.order.exception;

import java.util.UUID;

public class PaymentFailedException extends RuntimeException {

    private final UUID orderId;
    private final String failureReason;

    public PaymentFailedException(UUID orderId, String failureReason) {
        super(String.format("Payment failed for order %s: %s", orderId, failureReason));
        this.orderId = orderId;
        this.failureReason = failureReason;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
