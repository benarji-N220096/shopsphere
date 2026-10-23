package com.shopsphere.order.service;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.config.CurrentUserProvider;
import com.shopsphere.order.dto.CartItemResponse;
import com.shopsphere.order.dto.CartResponse;
import com.shopsphere.order.dto.CreateOrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.ProductDto;
import com.shopsphere.order.dto.inventory.ReservationItemRequest;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.inventory.ReserveInventoryRequest;
import com.shopsphere.order.dto.payment.PaymentRequest;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderStatus;
import com.shopsphere.order.exception.EmptyCartException;
import com.shopsphere.order.exception.InsufficientStockException;
import com.shopsphere.order.exception.InventoryServiceUnavailableException;
import com.shopsphere.order.exception.OrderNotFoundException;
import com.shopsphere.order.exception.PaymentFailedException;
import com.shopsphere.order.exception.PaymentServiceUnavailableException;
import com.shopsphere.order.exception.ProductNotFoundException;
import com.shopsphere.order.exception.ProductUnavailableException;
import com.shopsphere.order.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @Mock
    private CartClient cartClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UUID reservationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
    }

    private void mockCartAndProducts(UUID prodId1, UUID prodId2) {
        CartResponse cart = new CartResponse();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);

        CartItemResponse item1 = new CartItemResponse();
        item1.setId(UUID.randomUUID());
        item1.setProductId(prodId1);
        item1.setQuantity(2);

        CartItemResponse item2 = new CartItemResponse();
        item2.setId(UUID.randomUUID());
        item2.setProductId(prodId2);
        item2.setQuantity(3);

        cart.setItems(List.of(item1, item2));
        when(cartClient.getCart(userId)).thenReturn(cart);

        ProductDto p1 = new ProductDto();
        p1.setId(prodId1);
        p1.setName("Widget A");
        p1.setPrice(new BigDecimal("25.50"));
        p1.setActive(true);

        ProductDto p2 = new ProductDto();
        p2.setId(prodId2);
        p2.setName("Widget B");
        p2.setPrice(new BigDecimal("10.00"));
        p2.setActive(true);

        when(productClient.getProductById(prodId1)).thenReturn(p1);
        when(productClient.getProductById(prodId2)).thenReturn(p2);
    }

    @Test
    void testCreateOrder_paymentSuccess() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("SUCCESS");
        paymentResp.setTransactionId("TXN_123");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        when(orderPersistenceService.updateOrderStatus(any(), eq(OrderStatus.CONFIRMED))).thenAnswer(invocation -> {
            Order o = new Order();
            o.setId(invocation.getArgument(0));
            o.setUserId(userId);
            o.setStatus(OrderStatus.CONFIRMED);
            o.setTotalAmount(new BigDecimal("81.00"));
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");
        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        assertEquals(new BigDecimal("81.00"), response.getTotalAmount());

        // 1. Inventory reservation called
        verify(inventoryClient, times(1)).reserveInventory(any(ReserveInventoryRequest.class));
        // 2. Order persisted as PENDING
        verify(orderPersistenceService, times(1)).saveOrder(any(Order.class));
        // 3. Payment called
        verify(paymentClient, times(1)).processPayment(any(PaymentRequest.class));
        // 4. Inventory confirmed
        verify(inventoryClient, times(1)).confirmReservation(reservationId);
        // 5. Order updated to CONFIRMED
        verify(orderPersistenceService, times(1)).updateOrderStatus(any(), eq(OrderStatus.CONFIRMED));
        // 6. Cart cleared
        verify(cartClient, times(1)).clearCart(userId);
    }

    @Test
    void testCreateOrder_paymentDeclined() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("FAILED");
        paymentResp.setFailureReason("Card was declined by issuing bank");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "FAIL_DECLINED");
        PaymentFailedException ex = assertThrows(PaymentFailedException.class, () -> orderService.createOrder(request));
        assertEquals("Card was declined by issuing bank", ex.getFailureReason());

        // Verifies inventory reservation was cancelled
        verify(inventoryClient, times(1)).cancelReservation(reservationId);
        // Verifies order status was updated to CANCELLED
        verify(orderPersistenceService, times(1)).updateOrderStatus(any(), eq(OrderStatus.CANCELLED));
        // Verifies cart was NOT cleared
        verify(cartClient, never()).clearCart(any());
        // Verifies inventory was NOT confirmed
        verify(inventoryClient, never()).confirmReservation(any());
    }

    @Test
    void testCreateOrder_paymentInsufficientFunds() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("FAILED");
        paymentResp.setFailureReason("Insufficient funds in account");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        CreateOrderRequest request = new CreateOrderRequest("DEBIT_CARD", "FAIL_INSUFFICIENT_FUNDS");
        PaymentFailedException ex = assertThrows(PaymentFailedException.class, () -> orderService.createOrder(request));
        assertEquals("Insufficient funds in account", ex.getFailureReason());

        // Verifies inventory cancelled, order CANCELLED, cart kept
        verify(inventoryClient, times(1)).cancelReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(any(), eq(OrderStatus.CANCELLED));
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_paymentServiceUnavailable_throws503() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeignException serviceUnavailable = mock(FeignException.class);
        when(serviceUnavailable.status()).thenReturn(503);
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenThrow(serviceUnavailable);

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");
        assertThrows(PaymentServiceUnavailableException.class, () -> orderService.createOrder(request));

        // Order remains PENDING, inventory remains RESERVED (neither confirmed nor cancelled), cart not cleared
        verify(orderPersistenceService, never()).updateOrderStatus(any(), any());
        verify(inventoryClient, never()).cancelReservation(any());
        verify(inventoryClient, never()).confirmReservation(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_inventoryReservationFailure_paymentNeverCalled() {
        UUID prodId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        cart.setUserId(userId);
        CartItemResponse item = new CartItemResponse();
        item.setProductId(prodId);
        item.setQuantity(5);
        cart.setItems(List.of(item));

        when(cartClient.getCart(userId)).thenReturn(cart);

        ProductDto p = new ProductDto();
        p.setId(prodId);
        p.setPrice(new BigDecimal("20.00"));
        p.setActive(true);
        when(productClient.getProductById(prodId)).thenReturn(p);

        FeignException.Conflict conflict = mock(FeignException.Conflict.class);
        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenThrow(conflict);

        assertThrows(InsufficientStockException.class, () -> orderService.createOrder());

        // Verifies order persistence and payment were never called
        verify(orderPersistenceService, never()).saveOrder(any());
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_orderPersistenceFails_paymentNeverCalled() {
        UUID prodId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        cart.setUserId(userId);
        CartItemResponse item = new CartItemResponse();
        item.setProductId(prodId);
        item.setQuantity(2);
        cart.setItems(List.of(item));

        when(cartClient.getCart(userId)).thenReturn(cart);

        ProductDto p = new ProductDto();
        p.setId(prodId);
        p.setPrice(new BigDecimal("20.00"));
        p.setActive(true);
        when(productClient.getProductById(prodId)).thenReturn(p);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> orderService.createOrder());

        // Verifies compensating cancel is invoked
        verify(inventoryClient, times(1)).cancelReservation(reservationId);
        // Verifies payment was never called
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_confirmationFails_orderRemainsPending_cartNotCleared() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("SUCCESS");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        // Inventory confirm fails
        when(inventoryClient.confirmReservation(reservationId)).thenThrow(new RuntimeException("Inventory network error"));

        OrderResponse response = orderService.createOrder();

        // Order remains PENDING, cart is NOT cleared
        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getStatus());
        verify(orderPersistenceService, never()).updateOrderStatus(any(), eq(OrderStatus.CONFIRMED));
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_cartClearFails_orderConfirmed() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("SUCCESS");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        when(orderPersistenceService.updateOrderStatus(any(), eq(OrderStatus.CONFIRMED))).thenAnswer(invocation -> {
            Order o = new Order();
            o.setId(invocation.getArgument(0));
            o.setUserId(userId);
            o.setStatus(OrderStatus.CONFIRMED);
            o.setTotalAmount(new BigDecimal("81.00"));
            o.setCreatedAt(LocalDateTime.now());
            return o;
        });

        // Cart clear fails
        doThrow(new RuntimeException("Cart service down")).when(cartClient).clearCart(userId);

        OrderResponse response = orderService.createOrder();

        // Order is still CONFIRMED
        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(inventoryClient, times(1)).confirmReservation(reservationId);
        verify(orderPersistenceService, times(1)).updateOrderStatus(any(), eq(OrderStatus.CONFIRMED));
        verify(cartClient, times(1)).clearCart(userId);
    }

    @Test
    void testCreateOrder_idempotencyKeyAndAmountPropagation() {
        UUID prodId1 = UUID.randomUUID();
        UUID prodId2 = UUID.randomUUID();
        mockCartAndProducts(prodId1, prodId2);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("SUCCESS");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");
        orderService.createOrder(request);

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentClient).processPayment(captor.capture());

        PaymentRequest captured = captor.getValue();
        assertNotNull(captured.getOrderId());
        assertEquals("order-pay-" + captured.getOrderId(), captured.getIdempotencyKey());
        assertEquals(new BigDecimal("81.00"), captured.getAmount());
        assertEquals("USD", captured.getCurrency());
        assertEquals("CREDIT_CARD", captured.getPaymentMethod());
        assertEquals("SUCCESS", captured.getMockAction());
    }

    @Test
    void testCreateOrder_duplicateProductsInCart_aggregated() {
        UUID prodId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        cart.setUserId(userId);

        CartItemResponse item1 = new CartItemResponse();
        item1.setProductId(prodId);
        item1.setQuantity(2);

        CartItemResponse item2 = new CartItemResponse();
        item2.setProductId(prodId);
        item2.setQuantity(3);

        cart.setItems(List.of(item1, item2));
        when(cartClient.getCart(userId)).thenReturn(cart);

        ProductDto p = new ProductDto();
        p.setId(prodId);
        p.setName("Item");
        p.setPrice(new BigDecimal("10.00"));
        p.setActive(true);
        when(productClient.getProductById(prodId)).thenReturn(p);

        ReservationResponse reservation = new ReservationResponse();
        reservation.setReservationId(reservationId);
        reservation.setStatus("RESERVED");

        when(inventoryClient.reserveInventory(any(ReserveInventoryRequest.class))).thenReturn(reservation);
        when(orderPersistenceService.saveOrder(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResp = new PaymentResponse();
        paymentResp.setStatus("SUCCESS");
        when(paymentClient.processPayment(any(PaymentRequest.class))).thenReturn(paymentResp);

        orderService.createOrder();

        ArgumentCaptor<ReserveInventoryRequest> captor = ArgumentCaptor.forClass(ReserveInventoryRequest.class);
        verify(inventoryClient).reserveInventory(captor.capture());

        List<ReservationItemRequest> items = captor.getValue().getItems();
        assertEquals(1, items.size());
        assertEquals(5, items.get(0).getQuantity()); // 2 + 3 = 5
    }

    @Test
    void testCreateOrder_emptyCart_throwsEmptyCartException() {
        CartResponse cart = new CartResponse();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setItems(Collections.emptyList());

        when(cartClient.getCart(userId)).thenReturn(cart);

        assertThrows(EmptyCartException.class, () -> orderService.createOrder());
        verify(orderPersistenceService, never()).saveOrder(any());
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_nullCart_throwsEmptyCartException() {
        when(cartClient.getCart(userId)).thenReturn(null);

        assertThrows(EmptyCartException.class, () -> orderService.createOrder());
        verify(orderPersistenceService, never()).saveOrder(any());
        verify(paymentClient, never()).processPayment(any());
    }

    @Test
    void testCreateOrder_inactiveProduct_throwsProductUnavailableException() {
        UUID prodId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        cart.setUserId(userId);
        CartItemResponse item = new CartItemResponse();
        item.setProductId(prodId);
        item.setQuantity(1);
        cart.setItems(List.of(item));

        when(cartClient.getCart(userId)).thenReturn(cart);

        ProductDto p = new ProductDto();
        p.setId(prodId);
        p.setName("Discontinued Item");
        p.setPrice(new BigDecimal("50.00"));
        p.setActive(false);

        when(productClient.getProductById(prodId)).thenReturn(p);

        assertThrows(ProductUnavailableException.class, () -> orderService.createOrder());
        verify(orderPersistenceService, never()).saveOrder(any());
        verify(paymentClient, never()).processPayment(any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void testCreateOrder_productNotFound_throwsProductNotFoundException() {
        UUID prodId = UUID.randomUUID();
        CartResponse cart = new CartResponse();
        cart.setUserId(userId);
        CartItemResponse item = new CartItemResponse();
        item.setProductId(prodId);
        item.setQuantity(1);
        cart.setItems(List.of(item));

        when(cartClient.getCart(userId)).thenReturn(cart);

        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(productClient.getProductById(prodId)).thenThrow(notFound);

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder());
        verify(orderPersistenceService, never()).saveOrder(any());
        verify(paymentClient, never()).processPayment(any());
    }

    @Test
    void testGetOrdersForCurrentUser() {
        Order o1 = new Order();
        o1.setId(UUID.randomUUID());
        o1.setUserId(userId);
        o1.setTotalAmount(new BigDecimal("100.00"));
        o1.setStatus(OrderStatus.CONFIRMED);
        o1.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(o1));

        List<OrderResponse> orders = orderService.getOrdersForCurrentUser();
        assertEquals(1, orders.size());
        assertEquals(o1.getId(), orders.get(0).getId());
    }

    @Test
    void testGetOrderById_success() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setStatus(OrderStatus.CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(orderId);
        assertNotNull(response);
        assertEquals(orderId, response.getId());
    }

    @Test
    void testGetOrderById_notFound_throwsOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(orderId));
    }

    @Test
    void testGetOrderById_otherUser_throwsAccessDeniedException() {
        UUID orderId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Order otherOrder = new Order();
        otherOrder.setId(orderId);
        otherOrder.setUserId(otherUserId);

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(otherOrder));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> orderService.getOrderById(orderId));
    }
}
