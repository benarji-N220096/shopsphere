package com.shopsphere.order.scheduler;

import com.shopsphere.order.dto.BatchRecoveryResponse;
import com.shopsphere.order.service.OrderRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRecoverySchedulerTest {

    @Mock
    private OrderRecoveryService orderRecoveryService;

    private OrderRecoveryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OrderRecoveryScheduler(orderRecoveryService);
    }

    @Test
    void testRunScheduledRecovery_CallsRecoverStalePendingOrders() {
        BatchRecoveryResponse summary = new BatchRecoveryResponse(3, 1, 1, 1, 0);
        when(orderRecoveryService.recoverStalePendingOrders()).thenReturn(summary);

        scheduler.runScheduledRecovery();

        verify(orderRecoveryService, times(1)).recoverStalePendingOrders();
    }

    @Test
    void testRunScheduledRecovery_HandlesExceptionSafely() {
        when(orderRecoveryService.recoverStalePendingOrders())
                .thenThrow(new RuntimeException("Database connectivity lost"));

        // Should not throw or propagate exception, preserving scheduler continuity
        assertDoesNotThrow(() -> scheduler.runScheduledRecovery());

        verify(orderRecoveryService, times(1)).recoverStalePendingOrders();
    }
}
