package com.shopsphere.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.dto.ReservationItemRequest;
import com.shopsphere.inventory.dto.ReservationItemResponse;
import com.shopsphere.inventory.dto.ReservationResponse;
import com.shopsphere.inventory.dto.ReserveInventoryRequest;
import com.shopsphere.inventory.entity.ReservationStatus;
import com.shopsphere.inventory.exception.GlobalExceptionHandler;
import com.shopsphere.inventory.service.InventoryReservationPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.shopsphere.inventory.exception.ReservationNotFoundException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InventoryReservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryReservationPersistenceService reservationPersistenceService;

    @InjectMocks
    private InventoryReservationController reservationController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testReserve_Success_AndAggregatesDuplicateProducts() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        // Duplicate productId1 in input: 2 units and 3 units -> should aggregate to 5
        ReserveInventoryRequest request = new ReserveInventoryRequest(
                orderId,
                List.of(
                        new ReservationItemRequest(productId1, 2),
                        new ReservationItemRequest(productId1, 3),
                        new ReservationItemRequest(productId2, 1)
                )
        );

        ReservationResponse response = new ReservationResponse(
                UUID.randomUUID(),
                orderId,
                ReservationStatus.RESERVED,
                List.of(
                        new ReservationItemResponse(productId1, 5),
                        new ReservationItemResponse(productId2, 1)
                ),
                LocalDateTime.now(),
                null
        );

        when(reservationPersistenceService.createReservationAtomic(eq(orderId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<UUID, Integer>> captor = ArgumentCaptor.forClass(Map.class);
        verify(reservationPersistenceService).createReservationAtomic(eq(orderId), captor.capture());

        Map<UUID, Integer> capturedMap = captor.getValue();
        assertEquals(5, capturedMap.get(productId1));
        assertEquals(1, capturedMap.get(productId2));
    }

    @Test
    void testConfirm_Success() throws Exception {
        UUID reservationId = UUID.randomUUID();
        ReservationResponse response = new ReservationResponse(
                reservationId,
                UUID.randomUUID(),
                ReservationStatus.CONFIRMED,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(reservationPersistenceService.confirmReservationAtomic(reservationId))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations/{reservationId}/confirm", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void testCancel_Success() throws Exception {
        UUID reservationId = UUID.randomUUID();
        ReservationResponse response = new ReservationResponse(
                reservationId,
                UUID.randomUUID(),
                ReservationStatus.CANCELLED,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(reservationPersistenceService.cancelReservationAtomic(reservationId))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testReserve_InvalidRequest_Returns400() throws Exception {
        ReserveInventoryRequest request = new ReserveInventoryRequest(null, List.of());

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetReservationByOrderId_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        ReservationResponse response = new ReservationResponse(
                reservationId,
                orderId,
                ReservationStatus.RESERVED,
                List.of(),
                LocalDateTime.now(),
                null
        );

        when(reservationPersistenceService.getReservationByOrderId(orderId))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/reservations/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void testGetReservationByOrderId_NotFound_Returns404() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(reservationPersistenceService.getReservationByOrderId(orderId))
                .thenThrow(new ReservationNotFoundException("Reservation not found for order ID: " + orderId));

        mockMvc.perform(get("/api/v1/inventory/reservations/order/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testGetReservationByOrderId_InvalidUuid_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/reservations/order/invalid-uuid-string"))
                .andExpect(status().isBadRequest());
    }
}
