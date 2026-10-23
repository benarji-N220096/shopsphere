package com.shopsphere.inventory.service;

import com.shopsphere.inventory.client.ProductClient;
import com.shopsphere.inventory.dto.CreateInventoryRequest;
import com.shopsphere.inventory.dto.InventoryResponse;
import com.shopsphere.inventory.dto.ProductDto;
import com.shopsphere.inventory.dto.UpdateInventoryRequest;
import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.exception.InvalidInventoryQuantityException;
import com.shopsphere.inventory.exception.InventoryAlreadyExistsException;
import com.shopsphere.inventory.exception.InventoryNotFoundException;
import com.shopsphere.inventory.exception.ProductNotFoundException;
import com.shopsphere.inventory.exception.ProductUnavailableException;
import com.shopsphere.inventory.repository.InventoryRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryPersistenceService inventoryPersistenceService;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID productId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        productId = UUID.randomUUID();
    }

    @Test
    void testCreateInventory_success() {
        CreateInventoryRequest request = new CreateInventoryRequest(productId, 50);

        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);

        ProductDto product = new ProductDto(productId, "Test Product", true);
        when(productClient.getProductById(productId)).thenReturn(product);

        when(inventoryPersistenceService.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.createInventory(request);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals(50, response.getQuantity());
        verify(inventoryPersistenceService, times(1)).save(any(Inventory.class));
    }

    @Test
    void testCreateInventory_negativeQuantity_throwsException() {
        CreateInventoryRequest request = new CreateInventoryRequest(productId, -10);

        assertThrows(InvalidInventoryQuantityException.class, () -> inventoryService.createInventory(request));
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testCreateInventory_alreadyExists_throwsException() {
        CreateInventoryRequest request = new CreateInventoryRequest(productId, 50);
        when(inventoryRepository.existsByProductId(productId)).thenReturn(true);

        assertThrows(InventoryAlreadyExistsException.class, () -> inventoryService.createInventory(request));
        verify(productClient, never()).getProductById(any());
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testCreateInventory_productNotFound_throwsException() {
        CreateInventoryRequest request = new CreateInventoryRequest(productId, 50);
        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);

        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(notFound.status()).thenReturn(404);
        when(productClient.getProductById(productId)).thenThrow(notFound);

        assertThrows(ProductNotFoundException.class, () -> inventoryService.createInventory(request));
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testCreateInventory_inactiveProduct_throwsException() {
        CreateInventoryRequest request = new CreateInventoryRequest(productId, 50);
        when(inventoryRepository.existsByProductId(productId)).thenReturn(false);

        ProductDto product = new ProductDto(productId, "Inactive Product", false);
        when(productClient.getProductById(productId)).thenReturn(product);

        assertThrows(ProductUnavailableException.class, () -> inventoryService.createInventory(request));
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testGetInventory_success() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setQuantity(100);
        inventory.setCreatedAt(LocalDateTime.now());

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventory(productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals(100, response.getQuantity());
    }

    @Test
    void testGetInventory_notFound_throwsException() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.getInventory(productId));
    }

    @Test
    void testUpdateInventory_success() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setQuantity(50);
        inventory.setCreatedAt(LocalDateTime.now());

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryPersistenceService.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Absolute replacement: 50 -> 40
        UpdateInventoryRequest request = new UpdateInventoryRequest(40);
        InventoryResponse response = inventoryService.updateInventory(productId, request);

        assertNotNull(response);
        assertEquals(40, response.getQuantity());
        verify(inventoryPersistenceService, times(1)).save(inventory);
    }

    @Test
    void testUpdateInventory_negativeQuantity_throwsException() {
        UpdateInventoryRequest request = new UpdateInventoryRequest(-5);

        assertThrows(InvalidInventoryQuantityException.class, () -> inventoryService.updateInventory(productId, request));
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testUpdateInventory_notFound_throwsException() {
        UpdateInventoryRequest request = new UpdateInventoryRequest(40);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.updateInventory(productId, request));
        verify(inventoryPersistenceService, never()).save(any());
    }

    @Test
    void testDeleteInventory_success() {
        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProductId(productId);
        inventory.setQuantity(20);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        inventoryService.deleteInventory(productId);

        verify(inventoryPersistenceService, times(1)).delete(inventory);
    }

    @Test
    void testDeleteInventory_notFound_throwsException() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(InventoryNotFoundException.class, () -> inventoryService.deleteInventory(productId));
        verify(inventoryPersistenceService, never()).delete(any());
    }
}
