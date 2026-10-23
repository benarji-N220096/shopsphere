package com.shopsphere.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.dto.CreateOrderRequest;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.entity.OrderStatus;
import com.shopsphere.order.exception.GlobalExceptionHandler;
import com.shopsphere.order.exception.PaymentFailedException;
import com.shopsphere.order.exception.PaymentServiceUnavailableException;
import com.shopsphere.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateOrder_success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setStatus(OrderStatus.CONFIRMED);
        response.setTotalAmount(new BigDecimal("99.99"));
        response.setCreatedAt(LocalDateTime.now());
        response.setItems(Collections.emptyList());

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(99.99));
    }

    @Test
    void testCreateOrder_missingPaymentMethod_returns400() throws Exception {
        CreateOrderRequest invalid = new CreateOrderRequest(null, "SUCCESS");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_paymentFailed_returns400() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new PaymentFailedException(orderId, "Card was declined by issuing bank"));

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "FAIL_DECLINED");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Card was declined")));
    }

    @Test
    void testCreateOrder_paymentUnavailable_returns503() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new PaymentServiceUnavailableException("Payment service down"));

        CreateOrderRequest request = new CreateOrderRequest("CREDIT_CARD", "SUCCESS");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    void testGetOrders_success() throws Exception {
        when(orderService.getOrdersForCurrentUser()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetOrderById_success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setStatus(OrderStatus.CONFIRMED);
        response.setTotalAmount(new BigDecimal("49.50"));

        when(orderService.getOrderById(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.totalAmount").value(49.50));
    }
}
