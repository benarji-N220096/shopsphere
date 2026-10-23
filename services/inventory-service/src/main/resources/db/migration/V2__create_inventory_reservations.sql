ALTER TABLE inventory 
    ADD COLUMN reserved_quantity INT NOT NULL DEFAULT 0;

ALTER TABLE inventory
    ADD CONSTRAINT chk_inventory_reserved_quantity CHECK (reserved_quantity >= 0),
    ADD CONSTRAINT chk_inventory_available CHECK (quantity >= reserved_quantity);

CREATE TABLE inventory_reservations (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_reservations_order_id UNIQUE (order_id)
);

CREATE INDEX idx_reservations_order_id ON inventory_reservations (order_id);

CREATE TABLE inventory_reservation_items (
    id VARCHAR(36) PRIMARY KEY,
    reservation_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_res_items_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_reservations (id) ON DELETE CASCADE,
    CONSTRAINT chk_res_item_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_res_items_product_id ON inventory_reservation_items (product_id);
