USE cs202_ecommerce;

INSERT INTO Users (name, email, password_hash, role, is_active) VALUES
('Admin User', 'admin@shop.com', 'admin123', 'ADMIN', TRUE),
('Seller One', 'seller1@shop.com', 'seller123', 'SELLER', TRUE),
('Seller Two', 'seller2@shop.com', 'seller123', 'SELLER', TRUE),
('Customer One', 'customer1@shop.com', 'customer123', 'CUSTOMER', TRUE),
('Customer Two', 'customer2@shop.com', 'customer123', 'CUSTOMER', TRUE);

INSERT INTO Addresses (user_id, address_type, country, city, district, address_line, postal_code, phone, is_default) VALUES
(4, 'SHIPPING', 'Turkey', 'Istanbul', 'Kadikoy', 'Example Street 1', '34000', '5551112233', TRUE),
(4, 'BILLING', 'Turkey', 'Istanbul', 'Kadikoy', 'Example Street 1', '34000', '5551112233', TRUE),
(5, 'SHIPPING', 'Turkey', 'Ankara', 'Cankaya', 'Example Street 2', '06000', '5552223344', TRUE);

INSERT INTO Categories (category_name, description, created_by_admin) VALUES
('Electronics', 'Electronic devices', 1),
('Books', 'Books and magazines', 1),
('Clothing', 'Clothing products', 1);

INSERT INTO Catalogs (seller_id, catalog_name) VALUES
(2, 'Seller One Catalog'),
(3, 'Seller Two Catalog');

INSERT INTO Products (catalog_id, category_id, product_name, description, price, stock_quantity, is_active) VALUES
(1, 1, 'Laptop', 'Gaming laptop', 35000.00, 10, TRUE),
(1, 1, 'Headphones', 'Wireless headphones', 2500.00, 15, TRUE),
(2, 2, 'Novel Book', 'Classic novel', 200.00, 20, TRUE),
(2, 3, 'T-Shirt', 'Cotton T-shirt', 400.00, 30, TRUE);

INSERT INTO Orders (customer_id, seller_id, shipping_address_id, billing_address_id, status, total_amount) VALUES
(4, 2, 1, 2, 'DELIVERED', 37500.00),
(5, 3, 3, 3, 'PAID', 600.00);

INSERT INTO Order_Items (order_id, product_id, quantity, price_at_purchase) VALUES
(1, 1, 1, 35000.00),
(1, 2, 1, 2500.00),
(2, 4, 1, 400.00),
(2, 3, 1, 200.00);

INSERT INTO Payments (order_id, method, status, amount, paid_at) VALUES
(1, 'CREDIT_CARD', 'COMPLETED', 37500.00, NOW()),
(2, 'TRANSFER', 'COMPLETED', 600.00, NOW());

INSERT INTO Shipments (order_id, status, shipped_date, estimated_delivery_date, actual_delivery_date, tracking_number, carrier, updated_by_admin) VALUES
(1, 'DELIVERED', NOW(), NOW(), NOW(), 'TRK123456', 'DHL', 1),
(2, 'PENDING', NULL, NULL, NULL, NULL, NULL, 1);

INSERT INTO Reviews (order_item_id, customer_id, rating, comment) VALUES
(1, 4, 5, 'Excellent product'),
(2, 4, 4, 'Good quality');

INSERT INTO Coupons (code, discount_percent, is_active, created_by_admin) VALUES
('WELCOME10', 10, TRUE, 1),
('SPRING20', 20, TRUE, 1);

INSERT INTO Order_Coupons (order_id, coupon_id) VALUES
(1, 1);

INSERT INTO Wishlists (customer_id) VALUES
(4),
(5);

INSERT INTO Wishlist_Items (wishlist_id, product_id) VALUES
(1, 3),
(1, 4),
(2, 1);

INSERT INTO Notifications (user_id, title, message) VALUES
(4, 'Order Delivered', 'Your order has been delivered successfully.'),
(5, 'Order Payment', 'Your payment has been received.');
