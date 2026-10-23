package com.shopsphere.order.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateOrderRequest {

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String mockPaymentAction = "SUCCESS";

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String paymentMethod, String mockPaymentAction) {
        this.paymentMethod = paymentMethod;
        this.mockPaymentAction = mockPaymentAction;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMockPaymentAction() {
        return mockPaymentAction;
    }

    public void setMockPaymentAction(String mockPaymentAction) {
        this.mockPaymentAction = mockPaymentAction;
    }
}
