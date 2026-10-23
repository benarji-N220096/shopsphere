package com.shopsphere.cart.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CartMetrics {

    private final Counter cartItemsAddedCounter;
    private final Counter cartClearedCounter;

    public CartMetrics(MeterRegistry registry) {
        this.cartItemsAddedCounter = Counter.builder("cart.items.added")
                .description("Total number of items added to shopping carts")
                .tag("service", "cart-service")
                .register(registry);

        this.cartClearedCounter = Counter.builder("cart.cleared")
                .description("Total number of shopping carts cleared")
                .tag("service", "cart-service")
                .register(registry);
    }

    public void incrementItemsAdded() {
        cartItemsAddedCounter.increment();
    }

    public void incrementItemsAdded(int count) {
        if (count > 0) {
            cartItemsAddedCounter.increment(count);
        }
    }

    public void incrementCartCleared() {
        cartClearedCounter.increment();
    }
}
