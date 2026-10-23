package com.shopsphere.inventory.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @InjectMocks
    private InventoryReservationPersistenceService persistenceService;

    private UUID orderId;
    private UUID productId1;
    private UUID productId2;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productId1 = UUID.randomUUID();
        productId2 = UUID.randomUUID();

        inventory1 = new Inventory();
        inventory1.setId(UUID.randomUUID());
        inventory1.setProductId(productId1);
        inventory1.setQuantity(10);
        inventory1.setReservedQuantity(0);

        inventory2 = new Inventory();
        inventory2.setId(UUID.randomUUID());
        inventory2.setProductId(productId2);
        inventory2.setQuantity(5);
        inventory2.setReservedQuantity(0);
    }

    @Test
    void testReserve_Success() {
        Map<UUID, Integer> items = Map.of(productId1, 3);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdInWithLock(List.of(productId1))).thenReturn(List.of(inventory1));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = persistenceService.createReservationAtomic(orderId, items);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(ReservationStatus.RESERVED, response.getStatus());
        assertEquals(3, inventory1.getReservedQuantity());
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    void testReserve_InsufficientStock_Throws409() {
        Map<UUID, Integer> items = Map.of(productId1, 15); // only 10 available
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdInWithLock(List.of(productId1))).thenReturn(List.of(inventory1));

        assertThrows(InsufficientStockException.class, () ->
                persistenceService.createReservationAtomic(orderId, items));

        assertEquals(0, inventory1.getReservedQuantity());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testReserve_MultipleProducts_AllOrNothing() {
        // Product 1 has 10 (needs 4), Product 2 has 5 (needs 6 -> fails)
        Map<UUID, Integer> items = new LinkedHashMap<>();
        items.put(productId1, 4);
        items.put(productId2, 6);

        List<UUID> sorted = items.keySet().stream().sorted().toList();
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdInWithLock(sorted)).thenReturn(List.of(inventory1, inventory2));

        assertThrows(InsufficientStockException.class, () ->
                persistenceService.createReservationAtomic(orderId, items));

        // Ensure zero modifications were saved
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testReserve_DuplicateOrderId_Idempotent() {
        InventoryReservation existing = new InventoryReservation();
        existing.setId(UUID.randomUUID());
        existing.setOrderId(orderId);
        existing.setStatus(ReservationStatus.RESERVED);
        existing.setItems(Collections.emptyList());

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        Map<UUID, Integer> items = Map.of(productId1, 2);
        ReservationResponse response = persistenceService.createReservationAtomic(orderId, items);

        assertEquals(existing.getId(), response.getReservationId());
        verify(inventoryRepository, never()).findByProductIdInWithLock(any());
    }

    @Test
    void testReserve_ProductNotFound_ThrowsException() {
        Map<UUID, Integer> items = Map.of(productId1, 2);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(inventoryRepository.findByProductIdInWithLock(List.of(productId1))).thenReturn(Collections.emptyList());

        assertThrows(InventoryNotFoundException.class, () ->
                persistenceService.createReservationAtomic(orderId, items));
    }

    @Test
    void testConfirm_Success() {
        UUID reservationId = UUID.randomUUID();
        inventory1.setQuantity(10);
        inventory1.setReservedQuantity(3);

        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.RESERVED);
        InventoryReservationItem item = new InventoryReservationItem(UUID.randomUUID(), reservation, productId1, 3, LocalDateTime.now());
        reservation.setItems(List.of(item));

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductIdInWithLock(List.of(productId1))).thenReturn(List.of(inventory1));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = persistenceService.confirmReservationAtomic(reservationId);

        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        assertEquals(7, inventory1.getQuantity()); // 10 - 3
        assertEquals(0, inventory1.getReservedQuantity()); // 3 - 3
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    void testConfirm_Duplicate_Idempotent() {
        UUID reservationId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setItems(Collections.emptyList());

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        ReservationResponse response = persistenceService.confirmReservationAtomic(reservationId);

        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testConfirm_OnCancelledReservation_Throws400() {
        UUID reservationId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () ->
                persistenceService.confirmReservationAtomic(reservationId));
    }

    @Test
    void testCancel_Success() {
        UUID reservationId = UUID.randomUUID();
        inventory1.setQuantity(10);
        inventory1.setReservedQuantity(3);

        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.RESERVED);
        InventoryReservationItem item = new InventoryReservationItem(UUID.randomUUID(), reservation, productId1, 3, LocalDateTime.now());
        reservation.setItems(List.of(item));

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductIdInWithLock(List.of(productId1))).thenReturn(List.of(inventory1));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = persistenceService.cancelReservationAtomic(reservationId);

        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        assertEquals(10, inventory1.getQuantity()); // untouched
        assertEquals(0, inventory1.getReservedQuantity()); // 3 - 3
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    void testCancel_Duplicate_Idempotent() {
        UUID reservationId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setItems(Collections.emptyList());

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        ReservationResponse response = persistenceService.cancelReservationAtomic(reservationId);

        assertEquals(ReservationStatus.CANCELLED, response.getStatus());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testCancel_OnConfirmedReservation_Throws400() {
        UUID reservationId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(reservationId);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () ->
                persistenceService.cancelReservationAtomic(reservationId));
    }

    @Test
    void testReservation_NotFound_Throws404() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findByIdWithLock(reservationId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                persistenceService.confirmReservationAtomic(reservationId));
    }

    @Test
    void testGetReservationByOrderId_Success() {
        UUID testOrderId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(UUID.randomUUID());
        reservation.setOrderId(testOrderId);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setItems(Collections.emptyList());

        when(reservationRepository.findByOrderId(testOrderId)).thenReturn(Optional.of(reservation));

        ReservationResponse response = persistenceService.getReservationByOrderId(testOrderId);

        assertNotNull(response);
        assertEquals(testOrderId, response.getOrderId());
        assertEquals(ReservationStatus.RESERVED, response.getStatus());
        verify(reservationRepository).findByOrderId(testOrderId);
    }

    @Test
    void testGetReservationByOrderId_NotFound() {
        UUID testOrderId = UUID.randomUUID();
        when(reservationRepository.findByOrderId(testOrderId)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                persistenceService.getReservationByOrderId(testOrderId));
        verify(reservationRepository).findByOrderId(testOrderId);
    }
}
