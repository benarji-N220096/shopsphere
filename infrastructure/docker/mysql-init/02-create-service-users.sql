-- ShopSphere Least-Privilege Microservice Database Accounts
-- Ensures each microservice connects strictly using its dedicated user account
-- Enforces database ownership boundaries at the MySQL privilege level

-- 1. Auth Service User
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_auth`.* TO 'auth_user'@'%';

-- 2. Product Service User
CREATE USER IF NOT EXISTS 'product_user'@'%' IDENTIFIED BY 'product_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_product`.* TO 'product_user'@'%';

-- 3. Cart Service User
CREATE USER IF NOT EXISTS 'cart_user'@'%' IDENTIFIED BY 'cart_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_cart`.* TO 'cart_user'@'%';

-- 4. Order Service User
CREATE USER IF NOT EXISTS 'order_user'@'%' IDENTIFIED BY 'order_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_order`.* TO 'order_user'@'%';

-- 5. Inventory Service User
CREATE USER IF NOT EXISTS 'inventory_user'@'%' IDENTIFIED BY 'inventory_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_inventory`.* TO 'inventory_user'@'%';

-- 6. Payment Service User
CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass_shopsphere';
GRANT ALL PRIVILEGES ON `shopsphere_payment`.* TO 'payment_user'@'%';

FLUSH PRIVILEGES;
