import mysql.connector
import uuid
from datetime import datetime

# Database credentials
db_config = {
    'user': 'root',
    'password': 'bunny6281',
    'host': 'localhost',
    'port': 3306
}

new_products = [
    {
        'id': 'b1010001-0001-0001-0001-000000000001',
        'name': 'Flagship 5G AMOLED Smartphone (256GB)',
        'description': '6.7-inch 120Hz Dynamic AMOLED display, 200MP OIS pro camera system, and ultra-fast 5G connectivity.',
        'price': 54999.00,
        'category': 'Mobiles',
        'imageUrl': '/images/products/smartphone.jpg',
        'stock': 35
    },
    {
        'id': 'b1010002-0002-0002-0002-000000000002',
        'name': 'Pro Active Noise Cancellation AirPods',
        'description': 'Adaptive audio, active noise cancellation, transparency mode, and spatial audio with dynamic head tracking.',
        'price': 19900.00,
        'category': 'Audio',
        'imageUrl': '/images/products/airpods.jpg',
        'stock': 40
    },
    {
        'id': 'b1010003-0003-0003-0003-000000000003',
        'name': 'Wireless Bluetooth Sports Neckband',
        'description': 'Magnetic instant pairing, punchy bass, 30-hour playback, and IPX5 sweat-resistant silicone neckband.',
        'price': 1299.00,
        'category': 'Audio',
        'imageUrl': '/images/products/bluetooth_neckband.jpg',
        'stock': 65
    },
    {
        'id': 'b1010004-0004-0004-0004-000000000004',
        'name': 'Smart Voice Assistant Home Speaker',
        'description': '360-degree room-filling acoustic sound with smart voice control for music, smart home devices, and alarms.',
        'price': 3999.00,
        'category': 'Audio',
        'imageUrl': '/images/products/smart_speaker.jpg',
        'stock': 50
    },
    {
        'id': 'b1010005-0005-0005-0005-000000000005',
        'name': 'Luxury Chronograph Smartwatch (AMOLED, GPS)',
        'description': 'Sapphire glass dial, 1.4-inch high-nits AMOLED display, continuous heart rate tracking, and multi-sport GPS.',
        'price': 6499.00,
        'category': 'Wearables',
        'imageUrl': '/images/products/luxury_watch.jpg',
        'stock': 30
    },
    {
        'id': 'b1010006-0006-0006-0006-000000000006',
        'name': 'Frost-Free Double Door Smart Inverter Refrigerator (265L)',
        'description': 'Digital inverter compressor, multi-airflow cooling, toughened glass shelves, and 3-star energy efficiency.',
        'price': 26990.00,
        'category': 'Appliances',
        'imageUrl': '/images/products/refrigerator.jpg',
        'stock': 20
    },
    {
        'id': 'b1010007-0007-0007-0007-000000000007',
        'name': 'Fully Automatic Front Load Washing Machine (7.5 Kg)',
        'description': 'AI direct drive motor, steam hygiene wash, 1200 RPM spin speed, and 5-star energy rated efficiency.',
        'price': 28490.00,
        'category': 'Appliances',
        'imageUrl': '/images/products/washing_machine.jpg',
        'stock': 25
    },
    {
        'id': 'b1010008-0008-0008-0008-000000000008',
        'name': 'Aesthetic RGB Smart Corner Ambient Floor Lamp',
        'description': '16 million colors, music sync rhythm, app control, and minimalist modern corner design for bedroom or studio.',
        'price': 2199.00,
        'category': 'Home Decor',
        'imageUrl': '/images/products/aesthetic_lights.jpg',
        'stock': 45
    },
    {
        'id': 'b1010009-0009-0009-0009-000000000009',
        'name': '20000mAh 22.5W Fast Charging Power Bank',
        'description': 'High-density lithium polymer battery, dual USB-A + Type-C PD two-way fast charging with multi-device protection.',
        'price': 1699.00,
        'category': 'Accessories',
        'imageUrl': '/images/products/powerbank.jpg',
        'stock': 60
    }
]

try:
    conn_prod = mysql.connector.connect(database='shopsphere_product', **db_config)
    cursor_prod = conn_prod.cursor()

    now = datetime.now()

    for p in new_products:
        cursor_prod.execute(
            """
            INSERT INTO products (id, name, description, price, category, image_url, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                description = VALUES(description),
                price = VALUES(price),
                category = VALUES(category),
                image_url = VALUES(image_url),
                updated_at = VALUES(updated_at)
            """,
            (p['id'], p['name'], p['description'], p['price'], p['category'], p['imageUrl'], now, now)
        )
        print(f"Product upserted: {p['name']} (Rs {p['price']})")

    conn_prod.commit()
    cursor_prod.close()
    conn_prod.close()

    # Now upsert inventory
    conn_inv = mysql.connector.connect(database='shopsphere_inventory', **db_config)
    cursor_inv = conn_inv.cursor()

    for p in new_products:
        cursor_inv.execute(
            """
            INSERT INTO inventory (product_id, available_quantity, reserved_quantity, created_at, updated_at)
            VALUES (%s, %s, 0, %s, %s)
            ON DUPLICATE KEY UPDATE
                available_quantity = VALUES(available_quantity),
                updated_at = VALUES(updated_at)
            """,
            (p['id'], p['stock'], now, now)
        )
        print(f"Inventory upserted: {p['name']} -> {p['stock']} units")

    conn_inv.commit()
    cursor_inv.close()
    conn_inv.close()

    print("\nALL EXPANDED PRODUCTS AND INVENTORIES SUCCESSFULLY SAVED TO DATABASE!")
except Exception as e:
    print("Error:", e)
