package com.shopsphere.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderMetrics {

    private final Counter ordersCreatedCounter;
    private final Counter ordersConfirmedCounter;
    private final Counter ordersCancelledCounter;
    private final Counter ordersPendingCounter;
    private final Timer orderProcessingTimer;

    private final Counter recoverySuccessCounter;
    private final Counter recoveryFailedCounter;
    private final Counter recoveryPendingCounter;
    private final Timer recoveryDurationTimer;

    private final Timer ordersDurationTimer;
    private final Counter ordersRecoveryCyclesCounter;
    private final Counter ordersRecoveryOrdersFoundCounter;
    private final Counter ordersRecoveryOrdersConfirmedCounter;
    private final Counter ordersRecoveryOrdersCancelledCounter;
    private final Counter ordersRecoveryOrdersPendingCounter;
    private final Counter ordersRecoveryOrdersFailedCounter;

    public OrderMetrics(MeterRegistry registry) {
        this.ordersCreatedCounter = Counter.builder("orders.created")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(registry);

        this.ordersConfirmedCounter = Counter.builder("orders.confirmed")
                .description("Total number of orders confirmed")
                .tag("service", "order-service")
                .register(registry);

        this.ordersCancelledCounter = Counter.builder("orders.cancelled")
                .description("Total number of orders cancelled")
                .tag("service", "order-service")
                .register(registry);

        this.ordersPendingCounter = Counter.builder("orders.pending")
                .description("Total number of orders marked or remaining pending")
                .tag("service", "order-service")
                .register(registry);

        this.orderProcessingTimer = Timer.builder("order.processing.duration")
                .description("Time taken to process an order checkout")
                .tag("service", "order-service")
                .register(registry);

        this.ordersDurationTimer = Timer.builder("orders.duration")
                .description("Duration of order processing in seconds")
                .tag("service", "order-service")
                .register(registry);

        this.recoverySuccessCounter = Counter.builder("orders.recovery.success")
                .description("Total number of successful order recoveries")
                .tag("service", "order-service")
                .register(registry);

        this.recoveryFailedCounter = Counter.builder("orders.recovery.failed")
                .description("Total number of failed order recoveries")
                .tag("service", "order-service")
                .register(registry);

        this.recoveryPendingCounter = Counter.builder("orders.recovery.pending")
                .description("Total number of recoveries where order remained pending")
                .tag("service", "order-service")
                .register(registry);

        this.recoveryDurationTimer = Timer.builder("orders.recovery.duration")
                .description("Time taken to execute order recovery in seconds")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryCyclesCounter = Counter.builder("orders.recovery.cycles")
                .description("Total number of order recovery cycles initiated")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryOrdersFoundCounter = Counter.builder("orders.recovery.orders.found")
                .description("Total number of stale pending orders found during recovery")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryOrdersConfirmedCounter = Counter.builder("orders.recovery.orders.confirmed")
                .description("Total number of orders confirmed during recovery")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryOrdersCancelledCounter = Counter.builder("orders.recovery.orders.cancelled")
                .description("Total number of orders cancelled during recovery")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryOrdersPendingCounter = Counter.builder("orders.recovery.orders.pending")
                .description("Total number of orders remaining pending during recovery")
                .tag("service", "order-service")
                .register(registry);

        this.ordersRecoveryOrdersFailedCounter = Counter.builder("orders.recovery.orders.failed")
                .description("Total number of recovery operations that failed")
                .tag("service", "order-service")
                .register(registry);
    }

    public void incrementOrdersCreated() {
        ordersCreatedCounter.increment();
    }

    public void incrementOrdersConfirmed() {
        ordersConfirmedCounter.increment();
    }

    public void incrementOrdersCancelled() {
        ordersCancelledCounter.increment();
    }

    public void incrementOrdersPending() {
        ordersPendingCounter.increment();
    }

    public void recordOrderProcessingDuration(long durationMs) {
        orderProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        ordersDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void incrementRecoveryCycle() {
        ordersRecoveryCyclesCounter.increment();
    }

    public void recordOrdersFound(int count) {
        if (count > 0) {
            ordersRecoveryOrdersFoundCounter.increment(count);
        }
    }

    public void recordRecoveryConfirmed(int count) {
        if (count > 0) {
            ordersRecoveryOrdersConfirmedCounter.increment(count);
        }
    }

    public void recordRecoveryCancelled(int count) {
        if (count > 0) {
            ordersRecoveryOrdersCancelledCounter.increment(count);
        }
    }

    public void recordRecoveryPendingCount(int count) {
        if (count > 0) {
            ordersRecoveryOrdersPendingCounter.increment(count);
        }
    }

    public void recordRecoveryFailedCount(int count) {
        if (count > 0) {
            ordersRecoveryOrdersFailedCounter.increment(count);
        }
    }

    public void incrementRecoverySuccess() {
        recoverySuccessCounter.increment();
        ordersRecoveryOrdersConfirmedCounter.increment();
    }

    public void incrementRecoveryFailed() {
        recoveryFailedCounter.increment();
        ordersRecoveryOrdersFailedCounter.increment();
    }

    public void incrementRecoveryPending() {
        recoveryPendingCounter.increment();
        ordersRecoveryOrdersPendingCounter.increment();
    }

    public void recordRecoveryDuration(long durationMs) {
        recoveryDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
