package com.shopsphere.payment.processor;

import com.shopsphere.payment.dto.PaymentRequest;

public interface PaymentProcessor {

    PaymentResult process(PaymentRequest request);
}
