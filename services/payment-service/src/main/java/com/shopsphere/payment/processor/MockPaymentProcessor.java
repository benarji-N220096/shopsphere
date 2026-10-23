package com.shopsphere.payment.processor;

import com.shopsphere.payment.dto.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);

    public static final String ACTION_SUCCESS = "SUCCESS";
    public static final String ACTION_FAIL_DECLINED = "FAIL_DECLINED";
    public static final String ACTION_FAIL_INSUFFICIENT_FUNDS = "FAIL_INSUFFICIENT_FUNDS";

    @Override
    public PaymentResult process(PaymentRequest request) {
        String action = request.getMockAction();
        log.info("Processing mock payment for orderId: {}, amount: {}, requested action: {}",
                request.getOrderId(), request.getAmount(), action);

        if (ACTION_FAIL_DECLINED.equalsIgnoreCase(action)) {
            log.warn("Mock payment declined for orderId: {}", request.getOrderId());
            return PaymentResult.failed("Card was declined by issuing bank");
        }

        if (ACTION_FAIL_INSUFFICIENT_FUNDS.equalsIgnoreCase(action)) {
            log.warn("Mock payment failed due to insufficient funds for orderId: {}", request.getOrderId());
            return PaymentResult.failed("Insufficient funds in account");
        }

        // Default or SUCCESS: Generate mock transaction reference
        String transactionId = "TXN_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("Mock payment succeeded for orderId: {}, generated txnId: {}", request.getOrderId(), transactionId);
        return PaymentResult.success(transactionId);
    }
}
