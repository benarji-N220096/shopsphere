package com.shopsphere.payment.repository;

import com.shopsphere.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByOrderId(UUID orderId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    boolean existsByOrderId(UUID orderId);
}
