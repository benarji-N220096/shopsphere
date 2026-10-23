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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryPersistenceService inventoryPersistenceService;
    private final ProductClient productClient;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryPersistenceService inventoryPersistenceService,
                            ProductClient productClient) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryPersistenceService = inventoryPersistenceService;
        this.productClient = productClient;
    }

    public InventoryResponse getInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product ID: " + productId));
        return mapToResponse(inventory);
    }

    public InventoryResponse createInventory(CreateInventoryRequest request) {
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new InvalidInventoryQuantityException("Quantity cannot be negative");
        }

        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new InventoryAlreadyExistsException("Inventory already exists for product ID: " + request.getProductId());
        }

        // Remote validation outside of database transaction
        ProductDto product;
        try {
            product = productClient.getProductById(request.getProductId());
        } catch (FeignException.NotFound e) {
            throw new ProductNotFoundException("Product not found with ID: " + request.getProductId());
        }

        if (product == null) {
            throw new ProductNotFoundException("Product not found with ID: " + request.getProductId());
        }

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ProductUnavailableException("Product is inactive or unavailable: " + request.getProductId());
        }

        Inventory inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        inventory.setCreatedAt(LocalDateTime.now());

        // Local transaction boundary
        Inventory saved = inventoryPersistenceService.save(inventory);
        return mapToResponse(saved);
    }

    public InventoryResponse updateInventory(UUID productId, UpdateInventoryRequest request) {
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new InvalidInventoryQuantityException("Quantity cannot be negative");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product ID: " + productId));

        // The supplied quantity represents the new total available quantity (absolute value replacement)
        inventory.setQuantity(request.getQuantity());
        inventory.setUpdatedAt(LocalDateTime.now());

        Inventory saved = inventoryPersistenceService.save(inventory);
        return mapToResponse(saved);
    }

    public void deleteInventory(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for product ID: " + productId));

        inventoryPersistenceService.delete(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }
}
