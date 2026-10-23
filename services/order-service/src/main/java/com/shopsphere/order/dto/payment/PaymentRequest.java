package com.shopsphere.order.dto.payment;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentRequest {

    private UUID orderId;
    private UUID userId;
    private BigDecimal amount;
    private String currency = "USD";
    private String paymentMethod;
    private String idempotencyKey;
    private String mockAction;

    public PaymentRequest() {
    }

    public PaymentRequest(UUID orderId, UUID userId, BigDecimal amount, String currency, String paymentMethod, String idempotencyKey, String mockAction) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.mockAction = mockAction;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMockAction() {
        return mockAction;
    }

    public void setMockAction(String mockAction) {
        this.mockAction = mockAction;
    }
}
