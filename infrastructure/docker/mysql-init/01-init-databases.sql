-- ShopSphere Database Initialization Script
-- Initializes dedicated schemas for all 6 microservices
-- Preserves schema ownership and isolation

CREATE DATABASE IF NOT EXISTS `shopsphere_auth` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shopsphere_product` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shopsphere_cart` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shopsphere_order` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shopsphere_inventory` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `shopsphere_payment` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant permissions to root user from any host
GRANT ALL PRIVILEGES ON `shopsphere_auth`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `shopsphere_product`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `shopsphere_cart`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `shopsphere_order`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `shopsphere_inventory`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `shopsphere_payment`.* TO 'root'@'%';

FLUSH PRIVILEGES;
