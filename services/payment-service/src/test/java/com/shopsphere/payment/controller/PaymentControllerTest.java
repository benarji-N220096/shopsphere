package com.shopsphere.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.payment.dto.PaymentRequest;
import com.shopsphere.payment.dto.PaymentResponse;
import com.shopsphere.payment.entity.PaymentStatus;
import com.shopsphere.payment.exception.GlobalExceptionHandler;
import com.shopsphere.payment.exception.PaymentNotFoundException;
import com.shopsphere.payment.service.PaymentService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testProcessPayment_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("150.00"), "USD", "CREDIT_CARD", "idemp-test-1", "SUCCESS");

        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(), orderId, userId, new BigDecimal("150.00"), "USD",
                PaymentStatus.SUCCESS, "CREDIT_CARD", "TXN_MOCK_123", "idemp-test-1", null,
                LocalDateTime.now(), LocalDateTime.now());

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN_MOCK_123"));
    }

    @Test
    void testProcessPayment_InvalidInput_MissingFields_Returns400() throws Exception {
        PaymentRequest invalid = new PaymentRequest(null, null, BigDecimal.ZERO, null, null, null, null);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPaymentByOrderId_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentResponse response = new PaymentResponse(
                UUID.randomUUID(), orderId, UUID.randomUUID(), new BigDecimal("150.00"), "USD",
                PaymentStatus.SUCCESS, "CREDIT_CARD", "TXN_FOUND", "idemp-test-2", null,
                LocalDateTime.now(), LocalDateTime.now());

        when(paymentService.getPaymentByOrderId(eq(orderId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.transactionId").value("TXN_FOUND"));
    }

    @Test
    void testGetPaymentByOrderId_NotFound_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.getPaymentByOrderId(eq(orderId)))
                .thenThrow(new PaymentNotFoundException("Payment not found for order ID: " + orderId));

        mockMvc.perform(get("/api/v1/payments/order/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
