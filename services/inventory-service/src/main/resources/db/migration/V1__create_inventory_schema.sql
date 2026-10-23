CREATE TABLE inventory (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_inventory_product_id UNIQUE (product_id),
    CONSTRAINT chk_inventory_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_inventory_product_id ON inventory (product_id);
