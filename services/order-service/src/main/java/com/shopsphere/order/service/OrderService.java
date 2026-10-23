package com.shopsphere.order.service;

import com.shopsphere.order.client.CartClient;
import com.shopsphere.order.client.InventoryClient;
import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.client.ProductClient;
import com.shopsphere.order.config.CurrentUserProvider;
import com.shopsphere.order.dto.CartItemResponse;
import com.shopsphere.order.dto.CartResponse;
import com.shopsphere.order.dto.CreateOrderRequest;
import com.shopsphere.order.dto.OrderItemResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.dto.ProductDto;
import com.shopsphere.order.dto.inventory.ReservationItemRequest;
import com.shopsphere.order.dto.inventory.ReservationResponse;
import com.shopsphere.order.dto.inventory.ReserveInventoryRequest;
import com.shopsphere.order.dto.payment.PaymentRequest;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderItem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.shopsphere.order.metrics.OrderMetrics;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final CurrentUserProvider currentUserProvider;
    private final OrderMetrics orderMetrics;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        OrderPersistenceService orderPersistenceService,
                        CartClient cartClient,
                        ProductClient productClient,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient,
                        CurrentUserProvider currentUserProvider,
                        @Autowired(required = false) OrderMetrics orderMetrics) {
        this.orderRepository = orderRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.currentUserProvider = currentUserProvider;
        this.orderMetrics = orderMetrics;
    }

    public OrderService(OrderRepository orderRepository,
                        OrderPersistenceService orderPersistenceService,
                        CartClient cartClient,
                        ProductClient productClient,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient,
                        CurrentUserProvider currentUserProvider) {
        this(orderRepository, orderPersistenceService, cartClient, productClient, inventoryClient, paymentClient, currentUserProvider, null);
    }

    private void recordOrdersCreated() {
        if (orderMetrics != null) orderMetrics.incrementOrdersCreated();
    }

    private void recordOrdersConfirmed() {
        if (orderMetrics != null) orderMetrics.incrementOrdersConfirmed();
    }

    private void recordOrdersCancelled() {
        if (orderMetrics != null) orderMetrics.incrementOrdersCancelled();
    }

    private void recordOrdersPending() {
        if (orderMetrics != null) orderMetrics.incrementOrdersPending();
    }

    private void recordOrderDuration(long startMs) {
        if (orderMetrics != null) orderMetrics.recordOrderProcessingDuration(System.currentTimeMillis() - startMs);
    }

    /**
     * Creates an order from the current user's cart with atomic inventory reservation and payment orchestration.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        long checkoutStartMs = System.currentTimeMillis();
        try {
            return executeCreateOrder(request, checkoutStartMs);
        } finally {
            recordOrderDuration(checkoutStartMs);
        }
    }

    private OrderResponse executeCreateOrder(CreateOrderRequest request, long checkoutStartMs) {
        if (request == null) {
            request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");
        }

        // 1. Identify current user
        UUID userId = currentUserProvider.getCurrentUserId();
        log.info("Starting order placement for user {}", userId);

        // 2. Fetch current cart
        CartResponse cart = cartClient.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Cart is empty. Cannot create an order.");
        }

        // 3. Construct Order entity with pre-assigned ID
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // 4. Build OrderItems snapshot & aggregate required quantities by productId
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        Map<UUID, Integer> aggregatedQuantities = new LinkedHashMap<>();

        for (CartItemResponse item : cart.getItems()) {
            ProductDto product;
            try {
                product = productClient.getProductById(item.getProductId());
            } catch (FeignException.NotFound e) {
                throw new ProductNotFoundException("Product not found with ID: " + item.getProductId());
            }

            if (product == null) {
                throw new ProductNotFoundException("Product not found with ID: " + item.getProductId());
            }

            if (Boolean.FALSE.equals(product.getActive())) {
                throw new ProductUnavailableException("Product is inactive or unavailable: " + item.getProductId());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setId(UUID.randomUUID());
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setSubtotal(subtotal);
            orderItem.setCreatedAt(LocalDateTime.now());

            orderItems.add(orderItem);

            // Aggregate quantity by productId
            aggregatedQuantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // 5. Reserve stock via InventoryClient (Remote call - OUTSIDE DB transaction)
        List<ReservationItemRequest> reservationItems = aggregatedQuantities.entrySet().stream()
                .map(entry -> new ReservationItemRequest(entry.getKey(), entry.getValue()))
                .toList();

        ReserveInventoryRequest reserveRequest = new ReserveInventoryRequest(orderId, reservationItems);
        ReservationResponse reservation;

        try {
            log.info("Requesting inventory reservation for order {} with {} items", orderId, reservationItems.size());
            reservation = inventoryClient.reserveInventory(reserveRequest);
            log.info("EVENT=INVENTORY_RESERVED orderId={} reservationId={}", orderId, reservation.getReservationId());
        } catch (FeignException.Conflict e) {
            log.warn("Inventory reservation rejected for order {}: insufficient stock", orderId);
            throw new InsufficientStockException("Insufficient stock available for requested products");
        } catch (FeignException e) {
            log.error("Failed to communicate with Inventory Service for order {}: status {}", orderId, e.status());
            if (e.status() == 409) {
                throw new InsufficientStockException("Insufficient stock available for requested products");
            }
            throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable");
        } catch (Exception e) {
            log.error("Unexpected error during inventory reservation for order {}: {}", orderId, e.getMessage());
            throw new InventoryServiceUnavailableException("Inventory Service is currently unavailable");
        }

        // 6. Persist locally via OrderPersistenceService (@Transactional boundary)
        Order savedOrder;
        try {
            savedOrder = orderPersistenceService.saveOrder(order);
            log.info("EVENT=ORDER_CREATED orderId={} userId={} status=PENDING", savedOrder.getId(), userId);
            recordOrdersCreated();
        } catch (Exception ex) {
            // Local DB persistence failed! Compensate by cancelling the reservation.
            log.error("Local Order DB persistence failed for order {}. Compensating by cancelling reservation {}: {}",
                    orderId, reservation.getReservationId(), ex.getMessage());
            try {
                inventoryClient.cancelReservation(reservation.getReservationId());
                log.info("Successfully cancelled inventory reservation {} during compensation", reservation.getReservationId());
            } catch (Exception compEx) {
                log.error("CRITICAL: Failed to cancel inventory reservation {} during compensation: {}",
                        reservation.getReservationId(), compEx.getMessage());
            }
            throw ex;
        }

        // 7. Call Payment Service (Remote call - OUTSIDE DB transaction)
        String idempotencyKey = "order-pay-" + orderId;
        String paymentMethod = request.getPaymentMethod();
        String mockAction = request.getMockPaymentAction() != null ? request.getMockPaymentAction() : "SUCCESS";

        PaymentRequest paymentRequest = new PaymentRequest(
                orderId,
                userId,
                totalAmount,
                "USD",
                paymentMethod,
                idempotencyKey,
                mockAction
        );

        PaymentResponse paymentResponse;
        try {
            log.info("Calling Payment Service for order {} with amount {} and idempotency key {}", orderId, totalAmount, idempotencyKey);
            paymentResponse = paymentClient.processPayment(paymentRequest);
            log.info("Received response from Payment Service for order {}: status {}", orderId, paymentResponse.getStatus());
        } catch (FeignException e) {
            log.error("Payment Service communication error for order {}, reservationId {}: status {} - {}",
                    orderId, reservation.getReservationId(), e.status(), e.getMessage());
            recordOrdersPending();
            throw new PaymentServiceUnavailableException("Payment Service is currently unavailable for order " + orderId, e);
        } catch (Exception e) {
            log.error("Unexpected error contacting Payment Service for order {}, reservationId {}: {}",
                    orderId, reservation.getReservationId(), e.getMessage());
            recordOrdersPending();
            throw new PaymentServiceUnavailableException("Payment Service is currently unavailable for order " + orderId, e);
        }

        // 8. Process Payment result
        if ("SUCCESS".equalsIgnoreCase(paymentResponse.getStatus())) {
            log.info("EVENT=PAYMENT_SUCCESS orderId={} paymentId={}", orderId, paymentResponse.getPaymentId());
            // 8a. Confirm reservation
            boolean confirmationSucceeded = false;
            try {
                inventoryClient.confirmReservation(reservation.getReservationId());
                confirmationSucceeded = true;
                log.info("EVENT=INVENTORY_CONFIRMED orderId={} reservationId={}", orderId, reservation.getReservationId());
            } catch (Exception e) {
                log.error("EVENTUAL CONSISTENCY ALERT: Payment succeeded for order {} (paymentId: {}) but inventory confirmation failed for reservation {}: {}. Order remains PENDING. Inventory remains RESERVED.",
                        orderId, paymentResponse.getPaymentId(), reservation.getReservationId(), e.getMessage());
            }

            // 8b. ONLY if inventory confirmation succeeds, update Order status to CONFIRMED
            if (confirmationSucceeded) {
                Order updated = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                if (updated != null) {
                    savedOrder = updated;
                } else {
                    savedOrder.setStatus(OrderStatus.CONFIRMED);
                }
                log.info("EVENT=ORDER_CONFIRMED orderId={}", orderId);
                recordOrdersConfirmed();

                // 8c. Clear cart
                try {
                    cartClient.clearCart(userId);
                    log.info("EVENT=CART_CLEARED orderId={} userId={}", orderId, userId);
                } catch (Exception e) {
                    log.warn("Could not clear cart for user {} after order {} was confirmed: {}",
                            userId, orderId, e.getMessage());
                }
            } else {
                log.warn("EVENT=ORDER_PENDING orderId={} reason='Inventory confirmation failed'", orderId);
                recordOrdersPending();
            }

            return mapToOrderResponse(savedOrder);

        } else {
            // Payment FAILED (e.g. FAIL_DECLINED, FAIL_INSUFFICIENT_FUNDS)
            log.warn("EVENT=PAYMENT_FAILED orderId={} reason='{}'", orderId, paymentResponse.getFailureReason());

            // 8d. Cancel inventory reservation
            try {
                inventoryClient.cancelReservation(reservation.getReservationId());
                log.info("Successfully cancelled inventory reservation {} after payment failure", reservation.getReservationId());
            } catch (Exception e) {
                log.error("CRITICAL: Failed to cancel inventory reservation {} after payment failure for order {}: {}",
                        reservation.getReservationId(), orderId, e.getMessage());
            }

            // 8e. Update Order status to CANCELLED
            try {
                Order cancelled = orderPersistenceService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                if (cancelled != null) {
                    savedOrder = cancelled;
                } else {
                    savedOrder.setStatus(OrderStatus.CANCELLED);
                }
                log.info("EVENT=ORDER_CANCELLED orderId={}", orderId);
                recordOrdersCancelled();
            } catch (Exception e) {
                log.error("Failed to update order {} status to CANCELLED: {}", orderId, e.getMessage());
            }

            // 8f. Do NOT clear cart. Throw PaymentFailedException
            throw new PaymentFailedException(orderId, paymentResponse.getFailureReason());
        }
    }

    public OrderResponse createOrder() {
        return createOrder(new CreateOrderRequest("CREDIT_CARD", "SUCCESS"));
    }

    public List<OrderResponse> getOrdersForCurrentUser() {
        if (currentUserProvider.isAdmin()) {
            return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .map(this::mapToOrderResponse)
                    .collect(Collectors.toList());
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(UUID orderId) {
        if (currentUserProvider.isAdmin()) {
            return orderRepository.findById(orderId)
                    .map(this::mapToOrderResponse)
                    .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        Optional<Order> userOrderOpt = orderRepository.findByIdAndUserId(orderId, userId);
        if (userOrderOpt.isPresent()) {
            return mapToOrderResponse(userOrderOpt.get());
        }

        Optional<Order> anyOrderOpt = orderRepository.findById(orderId);
        if (anyOrderOpt.isPresent()) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not own this order");
        }

        throw new OrderNotFoundException("Order not found with ID: " + orderId);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

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
        return response;
    }
}
