package com.shopsphere.order.repository;

import com.shopsphere.order.entity.Order;
import com.shopsphere.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime threshold);
}
