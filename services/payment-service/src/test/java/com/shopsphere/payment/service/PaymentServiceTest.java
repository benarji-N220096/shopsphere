package com.shopsphere.payment.service;

import com.shopsphere.payment.dto.PaymentRequest;
import com.shopsphere.payment.dto.PaymentResponse;
import com.shopsphere.payment.entity.Payment;
import com.shopsphere.payment.entity.PaymentStatus;
import com.shopsphere.payment.exception.InvalidPaymentRequestException;
import com.shopsphere.payment.exception.PaymentNotFoundException;
import com.shopsphere.payment.processor.MockPaymentProcessor;
import com.shopsphere.payment.processor.PaymentProcessor;
import com.shopsphere.payment.processor.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private PaymentProcessor paymentProcessor;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;
    private UUID userId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        idempotencyKey = "idemp-key-12345";
    }

    @Test
    void testProcessPayment_Success() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("100.00"), "USD", "CREDIT_CARD", idempotencyKey, "SUCCESS");

        when(paymentPersistenceService.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentPersistenceService.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentProcessor.process(request)).thenReturn(PaymentResult.success("TXN_MOCK_TEST_123"));
        when(paymentPersistenceService.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("TXN_MOCK_TEST_123", response.getTransactionId());
        verify(paymentProcessor, times(1)).process(request);
        verify(paymentPersistenceService, times(1)).saveAndFlush(any(Payment.class));
        verify(paymentPersistenceService, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPayment_Declined() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("50.00"), "USD", "CREDIT_CARD", idempotencyKey, "FAIL_DECLINED");

        when(paymentPersistenceService.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentPersistenceService.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentProcessor.process(request)).thenReturn(PaymentResult.failed("Card was declined by issuing bank"));
        when(paymentPersistenceService.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("Card was declined by issuing bank", response.getFailureReason());
        verify(paymentProcessor, times(1)).process(request);
    }

    @Test
    void testProcessPayment_InsufficientFunds() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("500.00"), "USD", "CREDIT_CARD", idempotencyKey, "FAIL_INSUFFICIENT_FUNDS");

        when(paymentPersistenceService.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentPersistenceService.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentProcessor.process(request)).thenReturn(PaymentResult.failed("Insufficient funds in account"));
        when(paymentPersistenceService.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("Insufficient funds in account", response.getFailureReason());
    }

    @Test
    void testProcessPayment_SameIdempotencyKey_ReturnsExisting_WithoutCallingProcessor() {
        Payment existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(orderId);
        existing.setUserId(userId);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setStatus(PaymentStatus.SUCCESS);
        existing.setTransactionId("TXN_EXISTING");
        existing.setIdempotencyKey(idempotencyKey);
        existing.setCreatedAt(LocalDateTime.now());

        when(paymentPersistenceService.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("100.00"), "USD", "CREDIT_CARD", idempotencyKey, "SUCCESS");

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(existing.getId(), response.getPaymentId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("TXN_EXISTING", response.getTransactionId());

        // CRUCIAL: Verify processor was NEVER invoked
        verify(paymentProcessor, never()).process(any());
        verify(paymentPersistenceService, never()).saveAndFlush(any());
    }

    @Test
    void testProcessPayment_ConcurrentDuplicateRace_HandlesConstraintAndReturnsExisting() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("100.00"), "USD", "CREDIT_CARD", idempotencyKey, "SUCCESS");

        // First call returns empty, second call in catch block returns concurrent payment
        Payment concurrentPayment = new Payment();
        concurrentPayment.setId(UUID.randomUUID());
        concurrentPayment.setOrderId(orderId);
        concurrentPayment.setUserId(userId);
        concurrentPayment.setAmount(new BigDecimal("100.00"));
        concurrentPayment.setStatus(PaymentStatus.SUCCESS);
        concurrentPayment.setIdempotencyKey(idempotencyKey);
        concurrentPayment.setTransactionId("TXN_CONCURRENT");

        when(paymentPersistenceService.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty(), Optional.of(concurrentPayment));
        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Concurrent insert causes unique constraint violation
        when(paymentPersistenceService.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for idempotency key"));

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(concurrentPayment.getId(), response.getPaymentId());
        assertEquals("TXN_CONCURRENT", response.getTransactionId());

        // Processor was NOT called
        verify(paymentProcessor, never()).process(any());
    }

    @Test
    void testGetPaymentByOrderId_Success() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(new BigDecimal("75.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("TXN_FOUND");
        payment.setCreatedAt(LocalDateTime.now());

        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        assertNotNull(response);
        assertEquals(payment.getId(), response.getPaymentId());
        assertEquals("TXN_FOUND", response.getTransactionId());
    }

    @Test
    void testGetPaymentByOrderId_NotFound_Throws404() {
        when(paymentPersistenceService.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrderId(orderId));
    }

    @Test
    void testProcessPayment_InvalidAmount_ZeroOrNegative_ThrowsException() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, BigDecimal.ZERO, "USD", "CREDIT_CARD", idempotencyKey, "SUCCESS");

        assertThrows(InvalidPaymentRequestException.class, () -> paymentService.processPayment(request));
        verify(paymentProcessor, never()).process(any());
    }

    @Test
    void testProcessPayment_InvalidMockAction_ThrowsException() {
        PaymentRequest request = new PaymentRequest(
                orderId, userId, new BigDecimal("100.00"), "USD", "CREDIT_CARD", idempotencyKey, "INVALID_ACTION");

        assertThrows(InvalidPaymentRequestException.class, () -> paymentService.processPayment(request));
        verify(paymentProcessor, never()).process(any());
    }
}
