USE shopsphere_product;

INSERT INTO products (id, name, description, price, stock_quantity, category, image_url, active, created_at, updated_at)
VALUES
('b1010001-0001-0001-0001-000000000001', 'Flagship 5G AMOLED Smartphone (256GB)', '6.7-inch 120Hz Dynamic AMOLED display, 200MP OIS pro camera system, and ultra-fast 5G connectivity.', 54999.00, 35, 'Mobiles', '/images/products/smartphone.jpg', 1, NOW(), NOW()),
('b1010002-0002-0002-0002-000000000002', 'Pro Active Noise Cancellation AirPods', 'Adaptive audio, active noise cancellation, transparency mode, and spatial audio with dynamic head tracking.', 19900.00, 40, 'Audio', '/images/products/airpods.jpg', 1, NOW(), NOW()),
('b1010003-0003-0003-0003-000000000003', 'Wireless Bluetooth Sports Neckband', 'Magnetic instant pairing, punchy bass, 30-hour playback, and IPX5 sweat-resistant silicone neckband.', 1299.00, 65, 'Audio', '/images/products/bluetooth_neckband.jpg', 1, NOW(), NOW()),
('b1010004-0004-0004-0004-000000000004', 'Smart Voice Assistant Home Speaker', '360-degree room-filling acoustic sound with smart voice control for music, smart home devices, and alarms.', 3999.00, 50, 'Audio', '/images/products/smart_speaker.jpg', 1, NOW(), NOW()),
('b1010005-0005-0005-0005-000000000005', 'Luxury Chronograph Smartwatch (AMOLED, GPS)', 'Sapphire glass dial, 1.4-inch high-nits AMOLED display, continuous heart rate tracking, and multi-sport GPS.', 6499.00, 30, 'Wearables', '/images/products/luxury_watch.jpg', 1, NOW(), NOW()),
('b1010006-0006-0006-0006-000000000006', 'Frost-Free Double Door Smart Inverter Refrigerator (265L)', 'Digital inverter compressor, multi-airflow cooling, toughened glass shelves, and 3-star energy efficiency.', 26990.00, 20, 'Appliances', '/images/products/refrigerator.jpg', 1, NOW(), NOW()),
('b1010007-0007-0007-0007-000000000007', 'Fully Automatic Front Load Washing Machine (7.5 Kg)', 'AI direct drive motor, steam hygiene wash, 1200 RPM spin speed, and 5-star energy rated efficiency.', 28490.00, 25, 'Appliances', '/images/products/washing_machine.jpg', 1, NOW(), NOW()),
('b1010008-0008-0008-0008-000000000008', 'Aesthetic RGB Smart Corner Ambient Floor Lamp', '16 million colors, music sync rhythm, app control, and minimalist modern corner design for bedroom or studio.', 2199.00, 45, 'Home Decor', '/images/products/aesthetic_lights.jpg', 1, NOW(), NOW()),
('b1010009-0009-0009-0009-000000000009', '20000mAh 22.5W Fast Charging Power Bank', 'High-density lithium polymer battery, dual USB-A + Type-C PD two-way fast charging with multi-device protection.', 1699.00, 60, 'Accessories', '/images/products/powerbank.jpg', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    price = VALUES(price),
    stock_quantity = VALUES(stock_quantity),
    category = VALUES(category),
    image_url = VALUES(image_url),
    active = 1,
    updated_at = NOW();

USE shopsphere_inventory;

INSERT INTO inventory (id, product_id, quantity, reserved_quantity, created_at, updated_at)
VALUES
(UUID(), 'b1010001-0001-0001-0001-000000000001', 35, 0, NOW(), NOW()),
(UUID(), 'b1010002-0002-0002-0002-000000000002', 40, 0, NOW(), NOW()),
(UUID(), 'b1010003-0003-0003-0003-000000000003', 65, 0, NOW(), NOW()),
(UUID(), 'b1010004-0004-0004-0004-000000000004', 50, 0, NOW(), NOW()),
(UUID(), 'b1010005-0005-0005-0005-000000000005', 30, 0, NOW(), NOW()),
(UUID(), 'b1010006-0006-0006-0006-000000000006', 20, 0, NOW(), NOW()),
(UUID(), 'b1010007-0007-0007-0007-000000000007', 25, 0, NOW(), NOW()),
(UUID(), 'b1010008-0008-0008-0008-000000000008', 45, 0, NOW(), NOW()),
(UUID(), 'b1010009-0009-0009-0009-000000000009', 60, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    quantity = VALUES(quantity),
    updated_at = NOW();
