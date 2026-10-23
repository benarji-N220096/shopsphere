package com.shopsphere.payment.processor;

public class PaymentResult {

    private final boolean success;
    private final String transactionId;
    private final String failureReason;

    public PaymentResult(boolean success, String transactionId, String failureReason) {
        this.success = success;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }

    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null);
    }

    public static PaymentResult failed(String failureReason) {
        return new PaymentResult(false, null, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
