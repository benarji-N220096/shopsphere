package com.shopsphere.order.service;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.dto.BatchRecoveryResponse;
import com.shopsphere.order.dto.OrderItemResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderStatus;
import com.shopsphere.order.exception.OrderNotFoundException;
import com.shopsphere.order.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.shopsphere.order.metrics.OrderMetrics;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrderRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(OrderRecoveryService.class);

    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final CartClient cartClient;
    private final OrderMetrics orderMetrics;
    private final long gracePeriodMinutes;
    private final long staleOrderMinutes;

    @Autowired
    public OrderRecoveryService(OrderRepository orderRepository,
                                OrderPersistenceService orderPersistenceService,
                                PaymentClient paymentClient,
                                InventoryClient inventoryClient,
                                CartClient cartClient,
                                @Autowired(required = false) OrderMetrics orderMetrics,
                                @Value("${shopsphere.order.recovery.grace-period-minutes:15}") long gracePeriodMinutes,
                                @Value("${shopsphere.order.recovery.stale-order-minutes:5}") long staleOrderMinutes) {
        this.orderRepository = orderRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.paymentClient = paymentClient;
        this.inventoryClient = inventoryClient;
        this.cartClient = cartClient;
        this.orderMetrics = orderMetrics;
        this.gracePeriodMinutes = gracePeriodMinutes;
        this.staleOrderMinutes = staleOrderMinutes;
    }

    public OrderRecoveryService(OrderRepository orderRepository,
                                OrderPersistenceService orderPersistenceService,
                                PaymentClient paymentClient,
                                InventoryClient inventoryClient,
                                CartClient cartClient,
                                long gracePeriodMinutes,
                                long staleOrderMinutes) {
        this(orderRepository, orderPersistenceService, paymentClient, inventoryClient, cartClient, null, gracePeriodMinutes, staleOrderMinutes);
    }

    private void recordRecoverySuccess() {
        if (orderMetrics != null) orderMetrics.incrementRecoverySuccess();
    }

    private void recordRecoveryFailed() {
        if (orderMetrics != null) orderMetrics.incrementRecoveryFailed();
    }

    private void recordRecoveryPending() {
        if (orderMetrics != null) orderMetrics.incrementRecoveryPending();
    }

    private void recordRecoveryDuration(long startMs) {
        if (orderMetrics != null) orderMetrics.recordRecoveryDuration(System.currentTimeMillis() - startMs);
    }

    /**
     * Batch recovery orchestration for stale PENDING orders.
     * Calculates stale threshold, queries OrderRepository, iterates through orders,
     * calls recoverOrder(orderId), handles individual failures, and returns a summary.
     */
    public BatchRecoveryResponse recoverStalePendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleOrderMinutes);
        List<Order> staleOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, threshold);

        int totalFound = staleOrders.size();
        int confirmedCount = 0;
        int cancelledCount = 0;
        int stillPendingCount = 0;
        int failedCount = 0;

        log.info("Batch recovery started: found {} stale PENDING orders created before {}", totalFound, threshold);

        for (Order order : staleOrders) {
            try {
                OrderResponse response = recoverOrder(order.getId());
                if (response.getStatus() == OrderStatus.CONFIRMED) {
                    confirmedCount++;
                } else if (response.getStatus() == OrderStatus.CANCELLED) {
                    cancelledCount++;
                } else if (response.getStatus() == OrderStatus.PENDING) {
                    stillPendingCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Batch recovery failed for order {}: {}", order.getId(), e.getMessage());
            }
        }

        log.info("Batch recovery completed: totalFound={}, confirmed={}, cancelled={}, stillPending={}, failed={}",
                totalFound, confirmedCount, cancelledCount, stillPendingCount, failedCount);

        if (orderMetrics != null) {
            orderMetrics.incrementRecoveryCycle();
            orderMetrics.recordOrdersFound(totalFound);
            orderMetrics.recordRecoveryConfirmed(confirmedCount);
            orderMetrics.recordRecoveryCancelled(cancelledCount);
            orderMetrics.recordRecoveryPendingCount(stillPendingCount);
            orderMetrics.recordRecoveryFailedCount(failedCount);
        }

        return new BatchRecoveryResponse(totalFound, confirmedCount, cancelledCount, stillPendingCount, failedCount);
    }

    /**
     * Safely reconciles an order based on downstream Payment and Inventory reservation states.
     * Note: Remote Feign calls execute OUTSIDE local database transactions.
     * Only persistence transitions via OrderPersistenceService are transactional.
     */
    public OrderResponse recoverOrder(UUID orderId) {
        long startTime = System.currentTimeMillis();
        log.info("EVENT=RECOVERY_STARTED orderId={}", orderId);
        try {
            OrderResponse response = executeRecoverOrder(orderId);
            if (response.getStatus() == OrderStatus.CONFIRMED || response.getStatus() == OrderStatus.CANCELLED) {
                recordRecoverySuccess();
                log.info("EVENT=RECOVERY_COMPLETED orderId={} result={} durationMs={}",
                        orderId, response.getStatus(), (System.currentTimeMillis() - startTime));
            } else {
                recordRecoveryPending();
                log.info("EVENT=RECOVERY_PENDING orderId={} status={} durationMs={}",
                        orderId, response.getStatus(), (System.currentTimeMillis() - startTime));
            }
            return response;
        } catch (Exception e) {
            recordRecoveryFailed();
            log.error("EVENT=RECOVERY_FAILED orderId={} durationMs={} error={}",
                    orderId, (System.currentTimeMillis() - startTime), e.getMessage());
            throw e;
        } finally {
            recordRecoveryDuration(startTime);
        }
    }

    private OrderResponse executeRecoverOrder(UUID orderId) {
        log.info("Starting recovery check for order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // 1. Safe no-op on already terminal states
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order {} is already CONFIRMED. Recovery is a safe no-op.", orderId);
            return mapToOrderResponse(order);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order {} is already CANCELLED. Recovery is a safe no-op.", orderId);
            return mapToOrderResponse(order);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} has non-recoverable status {}. Recovery skipped.", orderId, order.getStatus());
            return mapToOrderResponse(order);
        }

        log.info("Order {} is PENDING (created at {}). Inspecting downstream payment state.",
                orderId, order.getCreatedAt());

        // 2. Query Payment Service
        PaymentResponse payment = null;
        boolean paymentNotFound = false;

        try {
            payment = paymentClient.getPaymentByOrderId(orderId);
            log.info("Retrieved payment for order {}: status={}, paymentId={}",
                    orderId,
                    payment != null ? payment.getStatus() : "null",
                    payment != null ? payment.getPaymentId() : "null");
        } catch (FeignException e) {
            if (e.status() == 404) {
                paymentNotFound = true;
                log.info("Payment record not found (HTTP 404) for order {}", orderId);
            } else {
                log.warn("Payment Service returned error status {} for order {}. Leaving Order PENDING for future retry.",
                        e.status(), orderId);
                return mapToOrderResponse(order);
            }
        } catch (Exception e) {
            log.warn("Payment Service communication error/timeout for order {}: {}. Leaving Order PENDING for future retry.",
                    orderId, e.getMessage());
            return mapToOrderResponse(order);
        }

        // 3. Query Inventory Service for existing reservation
        ReservationResponse reservation = null;
        boolean reservationNotFound = false;
        boolean inventoryServiceFailed = false;

        try {
            reservation = inventoryClient.getReservationByOrderId(orderId);
            log.info("Retrieved inventory reservation for order {}: status={}, reservationId={}",
                    orderId,
                    reservation != null ? reservation.getStatus() : "null",
                    reservation != null ? reservation.getReservationId() : "null");
        } catch (FeignException e) {
            if (e.status() == 404) {
                reservationNotFound = true;
                log.info("Inventory reservation not found (HTTP 404) for order {}", orderId);
            } else {
                inventoryServiceFailed = true;
                log.warn("Inventory Service returned error status {} for order {}.", e.status(), orderId);
            }
        } catch (Exception e) {
            inventoryServiceFailed = true;
            log.warn("Inventory Service communication error/timeout for order {}: {}.", orderId, e.getMessage());
        }

        // 4. Decision logic based on payment & inventory evaluation

        // CASE A: PAYMENT SUCCESS
        if (payment != null && "SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            log.info("Order recovery evaluation [CASE A - Payment SUCCESS]: orderId={}, reservationId={}",
                    orderId, reservation != null ? reservation.getReservationId() : "none");

            if (inventoryServiceFailed) {
                log.warn("Payment succeeded for order {}, but Inventory Service is currently unavailable. Leaving Order PENDING for future retry.", orderId);
                return mapToOrderResponse(order);
            }

            if (reservationNotFound || reservation == null) {
                log.error("CRITICAL INCONSISTENCY: Payment succeeded for order {} (paymentId={}), but inventory reservation was NOT FOUND. Leaving Order PENDING for manual resolution.",
                        orderId, payment.getPaymentId());
                return mapToOrderResponse(order);
            }

            String resStatus = reservation.getStatus();
            if ("CONFIRMED".equalsIgnoreCase(resStatus)) {
                log.info("Inventory reservation {} is already CONFIRMED for order {}. Proceeding to confirm Order.",
                        reservation.getReservationId(), orderId);
                Order confirmedOrder = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                attemptCartClear(order.getUserId(), orderId);
                return mapToOrderResponse(confirmedOrder != null ? confirmedOrder : order);
            } else if ("RESERVED".equalsIgnoreCase(resStatus)) {
                try {
                    log.info("Confirming inventory reservation {} for order {}", reservation.getReservationId(), orderId);
                    inventoryClient.confirmReservation(reservation.getReservationId());
                    log.info("Inventory reservation {} confirmed successfully for order {}", reservation.getReservationId(), orderId);
                } catch (Exception e) {
                    log.error("EVENTUAL CONSISTENCY ALERT: Payment succeeded for order {} but inventory confirmation failed for reservation {}: {}. Order remains PENDING for retry.",
                            orderId, reservation.getReservationId(), e.getMessage());
                    return mapToOrderResponse(order);
                }

                Order confirmedOrder = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                log.info("Order {} transitioned PENDING -> CONFIRMED", orderId);
                attemptCartClear(order.getUserId(), orderId);
                return mapToOrderResponse(confirmedOrder != null ? confirmedOrder : order);
            } else if ("CANCELLED".equalsIgnoreCase(resStatus)) {
                log.error("CRITICAL INCONSISTENCY: Payment succeeded for order {} but inventory reservation {} is CANCELLED! Leaving Order PENDING without destructive action.",
                        orderId, reservation.getReservationId());
                return mapToOrderResponse(order);
            } else {
                log.error("CRITICAL INCONSISTENCY: Payment succeeded for order {} but inventory reservation {} has unexpected status {}. Leaving Order PENDING.",
                        orderId, reservation.getReservationId(), resStatus);
                return mapToOrderResponse(order);
            }
        }

        // CASE B: PAYMENT FAILED
        if (payment != null && "FAILED".equalsIgnoreCase(payment.getStatus())) {
            log.info("Order recovery evaluation [CASE B - Payment FAILED]: orderId={}, failureReason={}",
                    orderId, payment.getFailureReason());

            if (reservation != null) {
                String resStatus = reservation.getStatus();
                if ("CONFIRMED".equalsIgnoreCase(resStatus)) {
                    log.error("CRITICAL INCONSISTENCY: Payment FAILED for order {}, but inventory reservation {} is CONFIRMED! Leaving Order PENDING for manual resolution.",
                            orderId, reservation.getReservationId());
                    return mapToOrderResponse(order);
                } else if ("RESERVED".equalsIgnoreCase(resStatus)) {
                    try {
                        log.info("Cancelling inventory reservation {} for order {}", reservation.getReservationId(), orderId);
                        inventoryClient.cancelReservation(reservation.getReservationId());
                        log.info("Successfully cancelled inventory reservation {} for order {}", reservation.getReservationId(), orderId);
                    } catch (Exception e) {
                        log.error("Failed to cancel inventory reservation {} during recovery of failed payment for order {}: {}",
                                reservation.getReservationId(), orderId, e.getMessage());
                    }
                } else if ("CANCELLED".equalsIgnoreCase(resStatus)) {
                    log.info("Inventory reservation {} is already CANCELLED for order {}. Safely continuing.",
                            reservation.getReservationId(), orderId);
                } else {
                    log.error("Unexpected reservation status {} for order {}. Leaving Order PENDING.",
                            resStatus, orderId);
                    return mapToOrderResponse(order);
                }
            } else {
                log.info("No inventory reservation exists for failed payment on order {}. Safely proceeding to cancel Order.", orderId);
            }

            Order cancelledOrder = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
            log.info("Order {} transitioned PENDING -> CANCELLED after payment failure", orderId);
            return mapToOrderResponse(cancelledOrder != null ? cancelledOrder : order);
        }

        // CASE C: PAYMENT NOT FOUND (HTTP 404 from Payment Service)
        if (paymentNotFound) {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(gracePeriodMinutes);
            boolean gracePeriodExpired = order.getCreatedAt() != null && order.getCreatedAt().isBefore(cutoff);

            log.info("Order recovery evaluation [CASE C - Payment NOT FOUND]: orderId={}, createdAt={}, cutoff={}, gracePeriodExpired={}",
                    orderId, order.getCreatedAt(), cutoff, gracePeriodExpired);

            if (!gracePeriodExpired) {
                log.info("Order {} is within grace period (created at {}, grace period: {} minutes). Leaving Order PENDING for future retry.",
                        orderId, order.getCreatedAt(), gracePeriodMinutes);
                return mapToOrderResponse(order);
            }

            log.warn("Order {} payment genuinely NOT FOUND and grace period ({} minutes) has elapsed. Cancelling order.",
                    orderId, gracePeriodMinutes);

            if (reservation != null && "RESERVED".equalsIgnoreCase(reservation.getStatus())) {
                try {
                    log.info("Cancelling inventory reservation {} for expired unpaid order {}",
                            reservation.getReservationId(), orderId);
                    inventoryClient.cancelReservation(reservation.getReservationId());
                    log.info("Successfully cancelled inventory reservation {} for order {}",
                            reservation.getReservationId(), orderId);
                } catch (Exception e) {
                    log.error("Failed to cancel inventory reservation {} for expired unpaid order {}: {}",
                            reservation.getReservationId(), orderId, e.getMessage());
                }
            }

            Order cancelledOrder = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
            log.info("Order {} transitioned PENDING -> CANCELLED after grace period expired", orderId);
            return mapToOrderResponse(cancelledOrder != null ? cancelledOrder : order);
        }

        // UNEXPECTED / UNHANDLED PAYMENT STATUS (e.g. INITIATED)
        log.warn("Order {} has unhandled payment status {}. Leaving Order PENDING for future retry.",
                orderId, payment.getStatus());
        return mapToOrderResponse(order);
    }

    private void attemptCartClear(UUID userId, UUID orderId) {
        try {
            log.info("Attempting to clear cart for user {} after order {} confirmed", userId, orderId);
            cartClient.clearCart(userId);
            log.info("Cart cleared successfully for user {} after order {}", userId, orderId);
        } catch (Exception e) {
            log.warn("Could not clear cart for user {} after order {} was confirmed: {}",
                    userId, orderId, e.getMessage());
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        if (order.getItems() != null) {
            List<OrderItemResponse> itemResponses = order.getItems().stream()
                    .map(item -> {
                        OrderItemResponse itemResponse = new OrderItemResponse();
                        itemResponse.setId(item.getId());
                        itemResponse.setProductId(item.getProductId());
                        itemResponse.setProductName(item.getProductName());
                        itemResponse.setUnitPrice(item.getUnitPrice());
                        itemResponse.setQuantity(item.getQuantity());
                        itemResponse.setSubtotal(item.getSubtotal());
                        itemResponse.setCreatedAt(item.getCreatedAt());
                        itemResponse.setUpdatedAt(item.getUpdatedAt());
                        return itemResponse;
                    }).collect(Collectors.toList());
            response.setItems(itemResponses);
        }

        return response;
    }
}
