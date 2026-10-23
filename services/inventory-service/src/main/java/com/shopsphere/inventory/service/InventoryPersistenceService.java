package com.shopsphere.inventory.service;

import com.shopsphere.inventory.entity.Inventory;
import com.shopsphere.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryPersistenceService {

    private final InventoryRepository inventoryRepository;

    public InventoryPersistenceService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Persists the Inventory record in the local database.
     * The @Transactional boundary is strictly scoped to this local persistence method.
     */
    @Transactional
    public Inventory save(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    /**
     * Deletes the Inventory record from the local database.
     * The @Transactional boundary is strictly scoped to this local deletion method.
     */
    @Transactional
    public void delete(Inventory inventory) {
        inventoryRepository.delete(inventory);
    }
}
