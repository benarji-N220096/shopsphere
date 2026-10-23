package com.shopsphere.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.dto.BatchRecoveryResponse;
import com.shopsphere.order.dto.OrderResponse;
import com.shopsphere.order.entity.OrderStatus;
import com.shopsphere.order.exception.GlobalExceptionHandler;
import com.shopsphere.order.exception.OrderNotFoundException;
import com.shopsphere.order.service.OrderRecoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderRecoveryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderRecoveryService orderRecoveryService;

    @InjectMocks
    private OrderRecoveryController orderRecoveryController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(orderRecoveryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testRecoverOrder_Success_Returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setUserId(UUID.randomUUID());
        response.setTotalAmount(new BigDecimal("100.00"));
        response.setStatus(OrderStatus.CONFIRMED);
        response.setCreatedAt(LocalDateTime.now());
        response.setItems(List.of());

        when(orderRecoveryService.recoverOrder(orderId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/{orderId}/recover", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderRecoveryService).recoverOrder(orderId);
    }

    @Test
    void testRecoverOrder_NotFound_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderRecoveryService.recoverOrder(orderId))
                .thenThrow(new OrderNotFoundException("Order not found with ID: " + orderId));

        mockMvc.perform(post("/api/v1/orders/{orderId}/recover", orderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(orderRecoveryService).recoverOrder(orderId);
    }

    @Test
    void testRecoverOrder_InvalidUuid_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/orders/invalid-uuid-string/recover")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecoverPendingOrders_ReturnsSummary() throws Exception {
        BatchRecoveryResponse response = new BatchRecoveryResponse(5, 2, 1, 2, 0);

        when(orderRecoveryService.recoverStalePendingOrders()).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders/recover-pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFound").value(5))
                .andExpect(jsonPath("$.confirmedCount").value(2))
                .andExpect(jsonPath("$.cancelledCount").value(1))
                .andExpect(jsonPath("$.stillPendingCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(0));

        verify(orderRecoveryService).recoverStalePendingOrders();
    }
}
