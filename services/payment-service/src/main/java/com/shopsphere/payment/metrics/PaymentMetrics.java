package com.shopsphere.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PaymentMetrics {

    private final Counter paymentsSuccessCounter;
    private final Counter paymentsFailedCounter;
    private final Timer paymentProcessingTimer;

    private final Counter paymentsProcessedCounter;
    private final Timer paymentsDurationTimer;

    public PaymentMetrics(MeterRegistry registry) {
        this.paymentsSuccessCounter = Counter.builder("payments.success")
                .description("Total number of successful payments")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentsFailedCounter = Counter.builder("payments.failed")
                .description("Total number of failed payments")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentProcessingTimer = Timer.builder("payment.processing.duration")
                .description("Time taken to process payment transactions")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentsProcessedCounter = Counter.builder("payments.processed")
                .description("Total number of payments processed")
                .tag("service", "payment-service")
                .register(registry);

        this.paymentsDurationTimer = Timer.builder("payments.duration")
                .description("Duration of payment processing in seconds")
                .tag("service", "payment-service")
                .register(registry);
    }

    public void incrementPaymentsSuccess() {
        paymentsSuccessCounter.increment();
        paymentsProcessedCounter.increment();
    }

    public void incrementPaymentsFailed() {
        paymentsFailedCounter.increment();
        paymentsProcessedCounter.increment();
    }

    public void recordPaymentProcessingDuration(long durationMs) {
        paymentProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        paymentsDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
