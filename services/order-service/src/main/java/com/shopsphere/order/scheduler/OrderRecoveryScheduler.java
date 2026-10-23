package com.shopsphere.order.scheduler;

import com.shopsphere.order.dto.BatchRecoveryResponse;
import com.shopsphere.order.service.OrderRecoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled trigger for periodic recovery of stale PENDING orders.
 * This component acts solely as a timer trigger and delegates all recovery
 * logic to OrderRecoveryService.
 */
@Component
public class OrderRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderRecoveryScheduler.class);

    private final OrderRecoveryService orderRecoveryService;

    public OrderRecoveryScheduler(OrderRecoveryService orderRecoveryService) {
        this.orderRecoveryService = orderRecoveryService;
    }

    @Scheduled(fixedDelayString = "${shopsphere.order.recovery.scheduler-fixed-delay-ms:60000}")
    public void runScheduledRecovery() {
        log.info("Starting scheduled recovery cycle for stale PENDING orders");
        try {
            BatchRecoveryResponse summary = orderRecoveryService.recoverStalePendingOrders();
            log.info("Scheduled recovery cycle completed: totalFound={}, confirmed={}, cancelled={}, stillPending={}, failed={}",
                    summary.getTotalFound(), summary.getConfirmedCount(), summary.getCancelledCount(),
                    summary.getStillPendingCount(), summary.getFailedCount());
        } catch (Exception e) {
            log.error("Unexpected error during scheduled order recovery cycle: {}", e.getMessage(), e);
        }
    }
}
