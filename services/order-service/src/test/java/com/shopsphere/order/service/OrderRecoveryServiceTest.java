package com.shopsphere.order.service;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
import com.shopsphere.order.entity.OrderStatus;
import com.shopsphere.order.exception.OrderNotFoundException;
import com.shopsphere.order.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRecoveryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private CartClient cartClient;

    private OrderRecoveryService recoveryService;

    private final long gracePeriodMinutes = 15;
    private final long staleOrderMinutes = 5;
    private UUID orderId;
    private UUID userId;
    private UUID reservationId;

    @BeforeEach
    void setUp() {
        recoveryService = new OrderRecoveryService(
                orderRepository,
                orderPersistenceService,
                paymentClient,
                inventoryClient,
                cartClient,
                gracePeriodMinutes,
                staleOrderMinutes
        );

        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
    }

    private Order buildOrder(OrderStatus status, LocalDateTime createdAt) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(status);
        order.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setProductId(UUID.randomUUID());
        item.setProductName("Test Item");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        order.setItems(new ArrayList<>(List.of(item)));

        return order;
    }

    // 1. Order not found -> throws OrderNotFoundException
    @Test
    void testRecoverOrder_orderNotFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> recoveryService.recoverOrder(orderId));

        verify(paymentClient, never()).getPaymentByOrderId(any());
        verify(inventoryClient, never()).getReservationByOrderId(any());
    }

    // 2. CONFIRMED order -> safe no-op
    @Test
    void testRecoverOrder_orderAlreadyConfirmed_safeNoOp() {
        Order order = buildOrder(OrderStatus.CONFIRMED, LocalDateTime.now().minusHours(1));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(paymentClient, never()).getPaymentByOrderId(any());
        verify(inventoryClient, never()).getReservationByOrderId(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 3. CANCELLED order -> safe no-op
    @Test
    void testRecoverOrder_orderAlreadyCancelled_safeNoOp() {
        Order order = buildOrder(OrderStatus.CANCELLED, LocalDateTime.now().minusHours(1));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(paymentClient, never()).getPaymentByOrderId(any());
        verify(inventoryClient, never()).getReservationByOrderId(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 4. PENDING + Payment SUCCESS + Inventory RESERVED -> confirm inventory -> Order CONFIRMED -> clear cart
    @Test
    void testRecoverOrder_pending_paymentSuccess_inventoryReserved_confirmsAndClearsCart() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order confirmedOrder = buildOrder(OrderStatus.CONFIRMED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(inventoryClient, times(1)).confirmReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CONFIRMED);
        verify(cartClient, times(1)).clearCart(userId);
    }

    // 5. PENDING + Payment SUCCESS + Inventory already CONFIRMED -> Order CONFIRMED -> clear cart (no double-confirm)
    @Test
    void testRecoverOrder_pending_paymentSuccess_inventoryAlreadyConfirmed_confirmsOrderAndClearsCart() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("CONFIRMED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order confirmedOrder = buildOrder(OrderStatus.CONFIRMED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(inventoryClient, never()).confirmReservation(any()); // safe no-op
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CONFIRMED);
        verify(cartClient, times(1)).clearCart(userId);
    }

    // 6. PENDING + Payment FAILED + Inventory RESERVED -> cancel inventory -> Order CANCELLED -> cart retained
    @Test
    void testRecoverOrder_pending_paymentFailed_inventoryReserved_cancelsInventoryAndCancelsOrder() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("FAILED");
        payment.setFailureReason("Insufficient funds");
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order cancelledOrder = buildOrder(OrderStatus.CANCELLED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED)).thenReturn(cancelledOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryClient, times(1)).cancelReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CANCELLED);
        verify(cartClient, never()).clearCart(any());
    }

    // 7. PENDING + Payment FAILED + Inventory already CANCELLED -> Order CANCELLED
    @Test
    void testRecoverOrder_pending_paymentFailed_inventoryAlreadyCancelled_cancelsOrder() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("FAILED");
        payment.setFailureReason("Card declined");
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("CANCELLED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order cancelledOrder = buildOrder(OrderStatus.CANCELLED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED)).thenReturn(cancelledOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryClient, never()).cancelReservation(any()); // safe no-op
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CANCELLED);
        verify(cartClient, never()).clearCart(any());
    }

    // 8. PENDING + Payment 404 + within grace period -> remains PENDING
    @Test
    void testRecoverOrder_pending_payment404_withinGracePeriod_remainsPending() {
        // Created 5 minutes ago (< 15 min grace period)
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(paymentClient.getPaymentByOrderId(orderId)).thenThrow(notFound);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(inventoryClient, never()).cancelReservation(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 9. PENDING + Payment 404 + grace period expired -> cancel inventory -> Order CANCELLED
    @Test
    void testRecoverOrder_pending_payment404_gracePeriodExpired_cancelsInventoryAndCancelsOrder() {
        // Created 25 minutes ago (> 15 min grace period)
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(25));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(paymentClient.getPaymentByOrderId(orderId)).thenThrow(notFound);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order cancelledOrder = buildOrder(OrderStatus.CANCELLED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED)).thenReturn(cancelledOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryClient, times(1)).cancelReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CANCELLED);
        verify(cartClient, never()).clearCart(any());
    }

    // 10. Payment Service timeout / 503 unavailable -> remains PENDING
    @Test
    void testRecoverOrder_paymentServiceTimeoutOrUnavailable_remainsPending() {
        // Even if order is old (> 15 min), a 503 is NOT treated as 404
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(30));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        FeignException serviceUnavailable = mock(FeignException.class);
        when(serviceUnavailable.status()).thenReturn(503);
        when(paymentClient.getPaymentByOrderId(orderId)).thenThrow(serviceUnavailable);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(inventoryClient, never()).getReservationByOrderId(any());
        verify(inventoryClient, never()).cancelReservation(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
    }

    // 11. Payment SUCCESS + Inventory Service unavailable -> remains PENDING
    @Test
    void testRecoverOrder_paymentSuccess_inventoryServiceUnavailable_remainsPending() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        FeignException serviceUnavailable = mock(FeignException.class);
        when(serviceUnavailable.status()).thenReturn(503);
        when(inventoryClient.getReservationByOrderId(orderId)).thenThrow(serviceUnavailable);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(inventoryClient, never()).confirmReservation(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 12. Payment SUCCESS + Inventory confirmation failure -> remains PENDING
    @Test
    void testRecoverOrder_paymentSuccess_inventoryConfirmationFailure_remainsPending() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        doThrow(new RuntimeException("Inventory confirmation network failure"))
                .when(inventoryClient).confirmReservation(reservationId);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 13. Recovery called again after Order CONFIRMED -> safe no-op
    @Test
    void testRecoverOrder_calledAgainAfterConfirmed_safeNoOp() {
        Order order = buildOrder(OrderStatus.CONFIRMED, LocalDateTime.now().minusHours(2));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(paymentClient, never()).getPaymentByOrderId(any());
        verify(inventoryClient, never()).getReservationByOrderId(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
    }

    // 14. Recovery called again after Order CANCELLED -> safe no-op
    @Test
    void testRecoverOrder_calledAgainAfterCancelled_safeNoOp() {
        Order order = buildOrder(OrderStatus.CANCELLED, LocalDateTime.now().minusHours(2));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(paymentClient, never()).getPaymentByOrderId(any());
        verify(inventoryClient, never()).getReservationByOrderId(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
    }

    // 15. Cart clear failure after successful confirmation -> Order remains CONFIRMED
    @Test
    void testRecoverOrder_cartClearFailureAfterConfirmation_orderRemainsConfirmed() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        Order confirmedOrder = buildOrder(OrderStatus.CONFIRMED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);

        doThrow(new RuntimeException("Cart service down")).when(cartClient).clearCart(userId);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(inventoryClient, times(1)).confirmReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CONFIRMED);
        verify(cartClient, times(1)).clearCart(userId);
    }

    // 16. Unexpected/inconsistent reservation state (Payment SUCCESS + Inventory CANCELLED) -> remains PENDING
    @Test
    void testRecoverOrder_unexpectedReservationState_remainsPendingWithoutDestructiveAction() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("CANCELLED"); // Critical inconsistency!
        when(inventoryClient.getReservationByOrderId(orderId)).thenReturn(reservation);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(inventoryClient, never()).confirmReservation(any());
        verify(inventoryClient, never()).cancelReservation(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 17. Clarification 7: Payment SUCCESS + Inventory reservation NOT FOUND -> remains PENDING
    @Test
    void testRecoverOrder_paymentSuccess_inventoryNotFound_remainsPending() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("SUCCESS");
        payment.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(inventoryClient.getReservationByOrderId(orderId)).thenThrow(notFound);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(inventoryClient, never()).confirmReservation(any());
        verify(inventoryClient, never()).cancelReservation(any());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(cartClient, never()).clearCart(any());
    }

    // 18. Clarification 4: Payment FAILED + Inventory reservation NOT FOUND -> Order CANCELLED, cart intact
    @Test
    void testRecoverOrder_paymentFailed_inventoryNotFound_cancelsOrder() {
        Order order = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(5));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentResponse payment = new PaymentResponse();
        payment.setStatus("FAILED");
        payment.setFailureReason("Card expired");
        when(paymentClient.getPaymentByOrderId(orderId)).thenReturn(payment);

        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(inventoryClient.getReservationByOrderId(orderId)).thenThrow(notFound);

        Order cancelledOrder = buildOrder(OrderStatus.CANCELLED, order.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED)).thenReturn(cancelledOrder);

        OrderResponse response = recoveryService.recoverOrder(orderId);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(inventoryClient, never()).cancelReservation(any()); // nothing to cancel
        verify(orderPersistenceService, times(1)).updateOrderStatus(orderId, OrderStatus.CANCELLED);
        verify(cartClient, never()).clearCart(any());
    }

    // 19. Batch recovery: queries stale PENDING orders, invokes recovery for each, continues on failure, calculates counts
    @Test
    void testRecoverStalePendingOrders_Success_CalculatesCountsAndContinuesOnFailure() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();

        Order order1 = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        order1.setId(id1);
        Order order2 = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        order2.setId(id2);
        Order order3 = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        order3.setId(id3);
        Order order4 = buildOrder(OrderStatus.PENDING, LocalDateTime.now().minusMinutes(10));
        order4.setId(id4);

        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any()))
                .thenReturn(List.of(order1, order2, order3, order4));

        // Order 1 -> Payment SUCCESS + Inventory CONFIRMED -> Order becomes CONFIRMED
        when(orderRepository.findById(id1)).thenReturn(Optional.of(order1));
        PaymentResponse pay1 = new PaymentResponse();
        pay1.setStatus("SUCCESS");
        pay1.setPaymentId(UUID.randomUUID());
        when(paymentClient.getPaymentByOrderId(id1)).thenReturn(pay1);
        ReservationResponse res1 = new ReservationResponse();
        res1.setReservationId(UUID.randomUUID());
        res1.setStatus("CONFIRMED");
        when(inventoryClient.getReservationByOrderId(id1)).thenReturn(res1);
        Order confOrder = buildOrder(OrderStatus.CONFIRMED, order1.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(id1, OrderStatus.CONFIRMED)).thenReturn(confOrder);

        // Order 2 -> Payment FAILED + Inventory CANCELLED -> Order becomes CANCELLED
        when(orderRepository.findById(id2)).thenReturn(Optional.of(order2));
        PaymentResponse pay2 = new PaymentResponse();
        pay2.setStatus("FAILED");
        when(paymentClient.getPaymentByOrderId(id2)).thenReturn(pay2);
        ReservationResponse res2 = new ReservationResponse();
        res2.setReservationId(UUID.randomUUID());
        res2.setStatus("CANCELLED");
        when(inventoryClient.getReservationByOrderId(id2)).thenReturn(res2);
        Order cancOrder = buildOrder(OrderStatus.CANCELLED, order2.getCreatedAt());
        when(orderPersistenceService.updateOrderStatus(id2, OrderStatus.CANCELLED)).thenReturn(cancOrder);

        // Order 3 -> Payment 404 within grace period -> Order remains PENDING
        when(orderRepository.findById(id3)).thenReturn(Optional.of(order3));
        FeignException notFound = mock(FeignException.class);
        when(notFound.status()).thenReturn(404);
        when(paymentClient.getPaymentByOrderId(id3)).thenThrow(notFound);
        ReservationResponse res3 = new ReservationResponse();
        res3.setReservationId(UUID.randomUUID());
        res3.setStatus("RESERVED");
        when(inventoryClient.getReservationByOrderId(id3)).thenReturn(res3);

        // Order 4 -> Throws exception during recovery -> counted as failed, batch continues
        when(orderRepository.findById(id4)).thenThrow(new RuntimeException("DB error on order 4"));

        com.shopsphere.order.dto.BatchRecoveryResponse summary = recoveryService.recoverStalePendingOrders();

        assertNotNull(summary);
        assertEquals(4, summary.getTotalFound());
        assertEquals(1, summary.getConfirmedCount());
        assertEquals(1, summary.getCancelledCount());
        assertEquals(1, summary.getStillPendingCount());
        assertEquals(1, summary.getFailedCount());

        verify(orderRepository).findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any());
    }

    // 20. Batch recovery: empty list returns zeros
    @Test
    void testRecoverStalePendingOrders_EmptyList_ReturnsZeros() {
        when(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.PENDING), any()))
                .thenReturn(List.of());

        com.shopsphere.order.dto.BatchRecoveryResponse summary = recoveryService.recoverStalePendingOrders();

        assertNotNull(summary);
        assertEquals(0, summary.getTotalFound());
        assertEquals(0, summary.getConfirmedCount());
        assertEquals(0, summary.getCancelledCount());
        assertEquals(0, summary.getStillPendingCount());
        assertEquals(0, summary.getFailedCount());
    }
}
