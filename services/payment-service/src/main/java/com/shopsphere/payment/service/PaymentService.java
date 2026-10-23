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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.shopsphere.payment.metrics.PaymentMetrics;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final Set<String> ALLOWED_MOCK_ACTIONS = Set.of(
            MockPaymentProcessor.ACTION_SUCCESS,
            MockPaymentProcessor.ACTION_FAIL_DECLINED,
            MockPaymentProcessor.ACTION_FAIL_INSUFFICIENT_FUNDS
    );

    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentProcessor paymentProcessor;
    private final PaymentMetrics paymentMetrics;

    @Autowired
    public PaymentService(PaymentPersistenceService paymentPersistenceService,
                          PaymentProcessor paymentProcessor,
                          @Autowired(required = false) PaymentMetrics paymentMetrics) {
        this.paymentPersistenceService = paymentPersistenceService;
        this.paymentProcessor = paymentProcessor;
        this.paymentMetrics = paymentMetrics;
    }

    public PaymentService(PaymentPersistenceService paymentPersistenceService,
                          PaymentProcessor paymentProcessor) {
        this(paymentPersistenceService, paymentProcessor, null);
    }

    /**
     * Processes a payment request with strict database-backed idempotency.
     * Guarantees the PaymentProcessor is never executed twice for the same idempotency key or order.
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        long startMs = System.currentTimeMillis();
        try {
            PaymentResponse response = executeProcessPayment(request);
            if (response.getStatus() == PaymentStatus.SUCCESS) {
                if (paymentMetrics != null) paymentMetrics.incrementPaymentsSuccess();
            } else if (response.getStatus() == PaymentStatus.FAILED) {
                if (paymentMetrics != null) paymentMetrics.incrementPaymentsFailed();
            }
            return response;
        } finally {
            if (paymentMetrics != null) {
                paymentMetrics.recordPaymentProcessingDuration(System.currentTimeMillis() - startMs);
            }
        }
    }

    private PaymentResponse executeProcessPayment(PaymentRequest request) {
        validateRequest(request);

        // 1. Check if a payment already exists for this idempotency key
        Optional<Payment> existingByKey = paymentPersistenceService.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingByKey.isPresent()) {
            log.info("Idempotent replay detected: Payment {} already exists for idempotency key {}. Returning existing record.",
                    existingByKey.get().getId(), request.getIdempotencyKey());
            return mapToResponse(existingByKey.get());
        }

        // 2. Check if a payment already exists for this orderId (one payment per order rule)
        Optional<Payment> existingByOrder = paymentPersistenceService.findByOrderId(request.getOrderId());
        if (existingByOrder.isPresent()) {
            log.info("Payment {} already exists for orderId {}. Returning existing record.",
                    existingByOrder.get().getId(), request.getOrderId());
            return mapToResponse(existingByOrder.get());
        }

        // 3. Persist initial INITIATED record
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setCreatedAt(LocalDateTime.now());

        Payment initiatedPayment;
        try {
            initiatedPayment = paymentPersistenceService.saveAndFlush(payment);
            log.info("Payment record {} initiated for orderId {}", initiatedPayment.getId(), request.getOrderId());
        } catch (DataIntegrityViolationException ex) {
            // Concurrent race condition: Another concurrent thread inserted with same idempotencyKey or orderId
            log.warn("Concurrent duplicate detected for idempotency key {} or orderId {}. Retrieving existing record.",
                    request.getIdempotencyKey(), request.getOrderId());

            Optional<Payment> concurrent = paymentPersistenceService.findByIdempotencyKey(request.getIdempotencyKey());
            if (concurrent.isPresent()) {
                return mapToResponse(concurrent.get());
            }
            Optional<Payment> concurrentOrder = paymentPersistenceService.findByOrderId(request.getOrderId());
            if (concurrentOrder.isPresent()) {
                return mapToResponse(concurrentOrder.get());
            }
            throw ex;
        }

        // 4. Invoke PaymentProcessor (called exactly once)
        PaymentResult result = paymentProcessor.process(request);

        // 5. Update record with terminal status (SUCCESS or FAILED)
        if (result.isSuccess()) {
            initiatedPayment.setStatus(PaymentStatus.SUCCESS);
            initiatedPayment.setTransactionId(result.getTransactionId());
            initiatedPayment.setUpdatedAt(LocalDateTime.now());
            log.info("EVENT=PAYMENT_SUCCESS paymentId={} orderId={} txnId={}", initiatedPayment.getId(), request.getOrderId(), result.getTransactionId());
        } else {
            initiatedPayment.setStatus(PaymentStatus.FAILED);
            initiatedPayment.setFailureReason(result.getFailureReason());
            initiatedPayment.setUpdatedAt(LocalDateTime.now());
            log.warn("EVENT=PAYMENT_FAILED paymentId={} orderId={} reason='{}'", initiatedPayment.getId(), request.getOrderId(), result.getFailureReason());
        }

        Payment finalPayment = paymentPersistenceService.save(initiatedPayment);
        return mapToResponse(finalPayment);
    }

    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentPersistenceService.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order ID: " + orderId));
        return mapToResponse(payment);
    }

    private void validateRequest(PaymentRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentRequestException("Payment amount must be greater than zero");
        }

        if (request.getMockAction() != null && !request.getMockAction().isBlank()) {
            if (!ALLOWED_MOCK_ACTIONS.contains(request.getMockAction().toUpperCase())) {
                throw new InvalidPaymentRequestException("Invalid mockAction: " + request.getMockAction() +
                        ". Allowed actions: " + ALLOWED_MOCK_ACTIONS);
            }
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getIdempotencyKey(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
