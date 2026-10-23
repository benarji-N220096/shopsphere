package com.shopsphere.inventory.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InventoryMetrics {

    private final Counter reservationSuccessCounter;
    private final Counter reservationFailedCounter;
    private final Counter reservationConfirmedCounter;
    private final Counter reservationCancelledCounter;

    private final Counter reservationsCreatedCounter;
    private final Counter reservationsConfirmedCounter;
    private final Counter reservationsCancelledCounter;

    public InventoryMetrics(MeterRegistry registry) {
        this.reservationSuccessCounter = Counter.builder("inventory.reservation.success")
                .description("Total number of successful inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationFailedCounter = Counter.builder("inventory.reservation.failed")
                .description("Total number of failed inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationConfirmedCounter = Counter.builder("inventory.reservation.confirmed")
                .description("Total number of confirmed inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationCancelledCounter = Counter.builder("inventory.reservation.cancelled")
                .description("Total number of cancelled inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationsCreatedCounter = Counter.builder("inventory.reservations.created")
                .description("Total number of inventory reservations created")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationsConfirmedCounter = Counter.builder("inventory.reservations.confirmed")
                .description("Total number of confirmed inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);

        this.reservationsCancelledCounter = Counter.builder("inventory.reservations.cancelled")
                .description("Total number of cancelled inventory reservations")
                .tag("service", "inventory-service")
                .register(registry);
    }

    public void incrementReservationSuccess() {
        reservationSuccessCounter.increment();
        reservationsCreatedCounter.increment();
    }

    public void incrementReservationFailed() {
        reservationFailedCounter.increment();
    }

    public void incrementReservationConfirmed() {
        reservationConfirmedCounter.increment();
        reservationsConfirmedCounter.increment();
    }

    public void incrementReservationCancelled() {
        reservationCancelledCounter.increment();
        reservationsCancelledCounter.increment();
    }
}
