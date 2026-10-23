package com.shopsphere.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.dto.CreateInventoryRequest;
import com.shopsphere.inventory.dto.InventoryResponse;
import com.shopsphere.inventory.dto.UpdateInventoryRequest;
import com.shopsphere.inventory.exception.GlobalExceptionHandler;
import com.shopsphere.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        InventoryController controller = new InventoryController(inventoryService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetInventory_success() throws Exception {
        UUID productId = UUID.randomUUID();
        InventoryResponse response = new InventoryResponse(UUID.randomUUID(), productId, 75, LocalDateTime.now(), null);

        when(inventoryService.getInventory(productId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/inventory/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(75));
    }

    @Test
    void testCreateInventory_success() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateInventoryRequest request = new CreateInventoryRequest(productId, 100);
        InventoryResponse response = new InventoryResponse(UUID.randomUUID(), productId, 100, LocalDateTime.now(), null);

        when(inventoryService.createInventory(any(CreateInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void testCreateInventory_negativeQuantity_validationFailure() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateInventoryRequest request = new CreateInventoryRequest(productId, -10);

        mockMvc.perform(post("/api/v1/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateInventory_success() throws Exception {
        UUID productId = UUID.randomUUID();
        UpdateInventoryRequest request = new UpdateInventoryRequest(40);
        InventoryResponse response = new InventoryResponse(UUID.randomUUID(), productId, 40, LocalDateTime.now(), LocalDateTime.now());

        when(inventoryService.updateInventory(eq(productId), any(UpdateInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/inventory/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(40));
    }

    @Test
    void testUpdateInventory_negativeQuantity_validationFailure() throws Exception {
        UUID productId = UUID.randomUUID();
        UpdateInventoryRequest request = new UpdateInventoryRequest(-1);

        mockMvc.perform(put("/api/v1/inventory/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteInventory_success() throws Exception {
        UUID productId = UUID.randomUUID();
        doNothing().when(inventoryService).deleteInventory(productId);

        mockMvc.perform(delete("/api/v1/inventory/" + productId))
                .andExpect(status().isNoContent());
    }
}
