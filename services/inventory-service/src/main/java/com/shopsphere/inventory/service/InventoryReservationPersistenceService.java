package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.ReservationItemResponse;
import com.shopsphere.inventory.dto.ReservationResponse;
import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.entity.InventoryReservation;
import com.shopsphere.inventory.entity.InventoryReservationItem;
import com.shopsphere.inventory.entity.ReservationStatus;
import com.shopsphere.inventory.exception.InsufficientStockException;
import com.shopsphere.inventory.exception.InvalidReservationStateException;
import com.shopsphere.inventory.exception.InventoryNotFoundException;
import com.shopsphere.inventory.exception.ReservationNotFoundException;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.inventory.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.shopsphere.inventory.metrics.InventoryMetrics;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class InventoryReservationPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationPersistenceService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryMetrics inventoryMetrics;

    @Autowired
    public InventoryReservationPersistenceService(InventoryRepository inventoryRepository,
                                                InventoryReservationRepository reservationRepository,
                                                @Autowired(required = false) InventoryMetrics inventoryMetrics) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryMetrics = inventoryMetrics;
    }

    public InventoryReservationPersistenceService(InventoryRepository inventoryRepository,
                                                InventoryReservationRepository reservationRepository) {
        this(inventoryRepository, reservationRepository, null);
    }

    /**
     * Atomically locks inventory rows, validates stock, updates reserved_quantity,
     * and persists the reservation in a single database transaction.
     */
    @Transactional
    public ReservationResponse createReservationAtomic(UUID orderId, Map<UUID, Integer> aggregatedItems) {
        // 1. Double check existing reservation for orderId (idempotent replay)
        Optional<InventoryReservation> existing = reservationRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Existing reservation found for orderId {}. Returning existing reservation {}",
                    orderId, existing.get().getId());
            return mapToResponse(existing.get());
        }

        // 2. Sort product IDs to enforce deterministic lock ordering (prevents deadlocks)
        List<UUID> sortedProductIds = aggregatedItems.keySet().stream().sorted().toList();

        // 3. Acquire pessimistic write locks
        List<Inventory> inventories = inventoryRepository.findByProductIdInWithLock(sortedProductIds);

        // 4. Verify all requested products exist in inventory
        if (inventories.size() != sortedProductIds.size()) {
            if (inventoryMetrics != null) inventoryMetrics.incrementReservationFailed();
            throw new InventoryNotFoundException("One or more products do not have an inventory record");
        }

        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        // 5. Verify available stock for ALL items (All-or-Nothing check)
        for (Map.Entry<UUID, Integer> entry : aggregatedItems.entrySet()) {
            Inventory inv = inventoryMap.get(entry.getKey());
            int available = inv.getQuantity() - inv.getReservedQuantity();
            if (available < entry.getValue()) {
                log.warn("Insufficient stock for product {}. Requested: {}, Available: {}",
                        entry.getKey(), entry.getValue(), available);
                if (inventoryMetrics != null) inventoryMetrics.incrementReservationFailed();
                throw new InsufficientStockException("Insufficient stock for product: " + entry.getKey() +
                        ". Requested: " + entry.getValue() + ", Available: " + available);
            }
        }

        // 6. Update reserved_quantity for all requested products
        for (Map.Entry<UUID, Integer> entry : aggregatedItems.entrySet()) {
            Inventory inv = inventoryMap.get(entry.getKey());
            inv.setReservedQuantity(inv.getReservedQuantity() + entry.getValue());
            inv.setUpdatedAt(LocalDateTime.now());
            inventoryRepository.save(inv);
        }

        // 7. Persist Reservation and items
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(UUID.randomUUID());
        reservation.setOrderId(orderId);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        List<InventoryReservationItem> items = aggregatedItems.entrySet().stream()
                .map(entry -> new InventoryReservationItem(
                        UUID.randomUUID(),
                        reservation,
                        entry.getKey(),
                        entry.getValue(),
                        LocalDateTime.now()
                )).toList();
        reservation.setItems(items);

        InventoryReservation saved = reservationRepository.save(reservation);
        if (inventoryMetrics != null) inventoryMetrics.incrementReservationSuccess();
        log.info("EVENT=INVENTORY_RESERVED reservationId={} orderId={}", saved.getId(), orderId);
        return mapToResponse(saved);
    }

    /**
     * Atomically confirms a reservation: decrements physical quantity and held reservedQuantity,
     * transitions status to CONFIRMED.
     * Idempotent: Calling confirm again on already CONFIRMED reservation is a safe no-op.
     */
    @Transactional
    public ReservationResponse confirmReservationAtomic(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            log.info("Reservation {} is already CONFIRMED. Returning existing state.", reservationId);
            return mapToResponse(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStateException("Cannot confirm already cancelled reservation: " + reservationId);
        }

        List<UUID> sortedProductIds = reservation.getItems().stream()
                .map(InventoryReservationItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Inventory> inventories = inventoryRepository.findByProductIdInWithLock(sortedProductIds);
        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        for (InventoryReservationItem item : reservation.getItems()) {
            Inventory inv = inventoryMap.get(item.getProductId());
            if (inv != null) {
                inv.setQuantity(inv.getQuantity() - item.getQuantity());
                inv.setReservedQuantity(inv.getReservedQuantity() - item.getQuantity());
                inv.setUpdatedAt(LocalDateTime.now());
                inventoryRepository.save(inv);
            }
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setUpdatedAt(LocalDateTime.now());
        InventoryReservation saved = reservationRepository.save(reservation);
        if (inventoryMetrics != null) inventoryMetrics.incrementReservationConfirmed();
        log.info("EVENT=INVENTORY_CONFIRMED reservationId={}", reservationId);
        return mapToResponse(saved);
    }

    /**
     * Atomically cancels a reservation: releases held reservedQuantity, transitions status to CANCELLED.
     * Idempotent: Calling cancel again on already CANCELLED reservation is a safe no-op.
     */
    @Transactional
    public ReservationResponse cancelReservationAtomic(UUID reservationId) {
        InventoryReservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with ID: " + reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            log.info("Reservation {} is already CANCELLED. Returning existing state.", reservationId);
            return mapToResponse(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException("Cannot cancel already confirmed reservation: " + reservationId);
        }

        List<UUID> sortedProductIds = reservation.getItems().stream()
                .map(InventoryReservationItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Inventory> inventories = inventoryRepository.findByProductIdInWithLock(sortedProductIds);
        Map<UUID, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Function.identity()));

        for (InventoryReservationItem item : reservation.getItems()) {
            Inventory inv = inventoryMap.get(item.getProductId());
            if (inv != null) {
                inv.setReservedQuantity(inv.getReservedQuantity() - item.getQuantity());
                inv.setUpdatedAt(LocalDateTime.now());
                inventoryRepository.save(inv);
            }
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(LocalDateTime.now());
        InventoryReservation saved = reservationRepository.save(reservation);
        if (inventoryMetrics != null) inventoryMetrics.incrementReservationCancelled();
        log.info("EVENT=INVENTORY_CANCELLED reservationId={}", reservationId);
        return mapToResponse(saved);
    }

    /**
     * Retrieves an existing inventory reservation by orderId.
     * Throws ReservationNotFoundException (404) if no reservation exists for the order.
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationByOrderId(UUID orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found for order ID: " + orderId));
        return mapToResponse(reservation);
    }

    public ReservationResponse mapToResponse(InventoryReservation reservation) {
        List<ReservationItemResponse> itemResponses = reservation.getItems().stream()
                .map(item -> new ReservationItemResponse(item.getProductId(), item.getQuantity()))
                .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getOrderId(),
                reservation.getStatus(),
                itemResponses,
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
