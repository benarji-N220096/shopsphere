package com.shopsphere.order.resilience;

import com.shopsphere.order.client.PaymentClient;
import com.shopsphere.order.dto.payment.PaymentRequest;
import com.shopsphere.order.dto.payment.PaymentResponse;
import com.shopsphere.order.exception.PaymentServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Primary
@Component
public class ResilientPaymentClient implements PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientPaymentClient.class);

    private final PaymentClient feignClient;

    public ResilientPaymentClient(@Qualifier("paymentFeignClient") PaymentClient feignClient) {
        this.feignClient = feignClient;
    }

    @Override
    @CircuitBreaker(name = "order-payment", fallbackMethod = "processPaymentFallback")
    // STRICT IDEMPOTENCY SAFETY: Intentionally NO @Retry on processPayment!
    // Transient timeouts leave order in PENDING state for Phase 3 recovery reconciliation.
    public PaymentResponse processPayment(PaymentRequest request) {
        return feignClient.processPayment(request);
    }

    public PaymentResponse processPaymentFallback(PaymentRequest request, Throwable t) {
        log.error("Resilience fallback triggered for Payment of order {} - cause: {}",
                request.getOrderId(), t.getMessage());
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-payment circuit breaker prevented payment for order {}", request.getOrderId());
            throw new PaymentServiceUnavailableException(
                    "Payment Service circuit is OPEN for order " + request.getOrderId() + ". Preserving PENDING state.", t);
        }
        throw new PaymentServiceUnavailableException(
                "Payment Service is currently unavailable for order " + request.getOrderId(), t);
    }

    @Override
    @CircuitBreaker(name = "order-payment", fallbackMethod = "getPaymentFallback")
    @Retry(name = "order-payment-query")
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        return feignClient.getPaymentByOrderId(orderId);
    }

    public PaymentResponse getPaymentFallback(UUID orderId, Throwable t) {
        if (t instanceof FeignException.NotFound || (t instanceof FeignException fe && fe.status() == 404)) {
            throw (FeignException) t;
        }
        if (t instanceof CallNotPermittedException) {
            log.warn("CIRCUIT_OPEN: order-payment circuit breaker prevented getPayment for order {}", orderId);
            throw new PaymentServiceUnavailableException(
                    "Payment Service circuit is OPEN during recovery query for order " + orderId, t);
        }
        throw new PaymentServiceUnavailableException(
                "Payment Service is currently unavailable for order " + orderId, t);
    }
}
