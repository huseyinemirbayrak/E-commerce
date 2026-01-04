SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS cs202_ecommerce;
CREATE DATABASE cs202_ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cs202_ecommerce;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE Users (
  user_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(120) NOT NULL,
  email             VARCHAR(190) NOT NULL,
  password_hash     VARCHAR(255) NOT NULL,
  role              ENUM('CUSTOMER','SELLER','ADMIN') NOT NULL,
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE Addresses (
  address_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT NOT NULL,
  address_type      ENUM('SHIPPING','BILLING') NOT NULL,
  country           VARCHAR(80)  NOT NULL,
  city              VARCHAR(80)  NOT NULL,
  district          VARCHAR(120) NULL,
  address_line      VARCHAR(255) NOT NULL,
  postal_code       VARCHAR(20)  NULL,
  phone             VARCHAR(30)  NULL,
  is_default        BOOLEAN NOT NULL DEFAULT FALSE,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_addresses_user
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_addresses_user ON Addresses(user_id);

CREATE TABLE Categories (
  category_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_name     VARCHAR(120) NOT NULL,
  description       VARCHAR(255) NULL,
  created_by_admin  BIGINT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_categories_name (category_name),
  CONSTRAINT fk_categories_admin
    FOREIGN KEY (created_by_admin) REFERENCES Users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Catalogs (
  catalog_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id         BIGINT NOT NULL,
  catalog_name      VARCHAR(160) NOT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uq_catalogs_seller (seller_id),

  CONSTRAINT fk_catalogs_seller
    FOREIGN KEY (seller_id) REFERENCES Users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Products (
  product_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  catalog_id        BIGINT NOT NULL,
  category_id       BIGINT NOT NULL,

  product_name      VARCHAR(180) NOT NULL,
  description       TEXT NULL,
  price             DECIMAL(10,2) NOT NULL CHECK (price >= 0),
  stock_quantity    INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),

  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_products_catalog
    FOREIGN KEY (catalog_id) REFERENCES Catalogs(catalog_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES Categories(category_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_products_catalog  ON Products(catalog_id);
CREATE INDEX ix_products_category ON Products(category_id);

CREATE TABLE Orders (
  order_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id       BIGINT NOT NULL,
  seller_id         BIGINT NULL, 
  shipping_address_id BIGINT NULL,
  billing_address_id  BIGINT NULL,

  status            ENUM('ONGOING','PENDING','PAID','SHIPPED','DELIVERED','CANCELED') NOT NULL DEFAULT 'ONGOING',
  order_date        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_at      TIMESTAMP NULL,
  total_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),

  ongoing_customer_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN status='ONGOING' THEN customer_id ELSE NULL END) STORED,

  CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES Users(user_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  CONSTRAINT fk_orders_seller
    FOREIGN KEY (seller_id) REFERENCES Users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

  CONSTRAINT fk_orders_ship_addr
    FOREIGN KEY (shipping_address_id) REFERENCES Addresses(address_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

  CONSTRAINT fk_orders_bill_addr
    FOREIGN KEY (billing_address_id) REFERENCES Addresses(address_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

  UNIQUE KEY uq_one_ongoing_per_customer (ongoing_customer_id)
) ENGINE=InnoDB;

CREATE INDEX ix_orders_customer ON Orders(customer_id);
CREATE INDEX ix_orders_seller   ON Orders(seller_id);
CREATE INDEX ix_orders_status   ON Orders(status);

CREATE TABLE Order_Items (
  order_item_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id          BIGINT NOT NULL,
  product_id        BIGINT NOT NULL,

  quantity          INT NOT NULL CHECK (quantity > 0),
  price_at_purchase DECIMAL(10,2) NOT NULL CHECK (price_at_purchase >= 0),

  subtotal          DECIMAL(12,2)
    GENERATED ALWAYS AS (quantity * price_at_purchase) STORED,

  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_orderitems_order
    FOREIGN KEY (order_id) REFERENCES Orders(order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_orderitems_product
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  UNIQUE KEY uq_order_product (order_id, product_id)
) ENGINE=InnoDB;

CREATE INDEX ix_orderitems_order   ON Order_Items(order_id);
CREATE INDEX ix_orderitems_product ON Order_Items(product_id);

CREATE TABLE Payments (
  payment_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id          BIGINT NOT NULL,
  method            ENUM('CREDIT_CARD','TRANSFER','WALLET','CASH_ON_DELIVERY') NOT NULL,
  status            ENUM('PENDING','COMPLETED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
  amount            DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
  paid_at           TIMESTAMP NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_payments_order
    FOREIGN KEY (order_id) REFERENCES Orders(order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  UNIQUE KEY uq_payments_order (order_id)
) ENGINE=InnoDB;

CREATE INDEX ix_payments_status ON Payments(status);

CREATE TABLE Shipments (
  shipment_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id          BIGINT NOT NULL,

  status            ENUM('PENDING','PREPARING','SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','FAILED','CANCELED')
                    NOT NULL DEFAULT 'PENDING',

  shipped_date             TIMESTAMP NULL,
  estimated_delivery_date  TIMESTAMP NULL,
  actual_delivery_date     TIMESTAMP NULL,

  tracking_number   VARCHAR(80) NULL,
  carrier           VARCHAR(80) NULL,

  updated_by_admin  BIGINT NULL,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_shipments_order
    FOREIGN KEY (order_id) REFERENCES Orders(order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_shipments_admin
    FOREIGN KEY (updated_by_admin) REFERENCES Users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

  UNIQUE KEY uq_shipments_order (order_id)
) ENGINE=InnoDB;

CREATE INDEX ix_shipments_status ON Shipments(status);

CREATE TABLE Reviews (
  review_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_item_id     BIGINT NOT NULL,
  customer_id       BIGINT NOT NULL,
  rating            INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment           TEXT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_reviews_orderitem
    FOREIGN KEY (order_item_id) REFERENCES Order_Items(order_item_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_reviews_customer
    FOREIGN KEY (customer_id) REFERENCES Users(user_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

  UNIQUE KEY uq_one_review_per_item (order_item_id)
) ENGINE=InnoDB;

CREATE INDEX ix_reviews_customer ON Reviews(customer_id);

CREATE TABLE Coupons (
  coupon_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code              VARCHAR(50) NOT NULL,
  discount_percent  INT NOT NULL CHECK (discount_percent BETWEEN 1 AND 90),
  is_active         BOOLEAN NOT NULL DEFAULT TRUE,
  valid_from        TIMESTAMP NULL,
  valid_until       TIMESTAMP NULL,
  created_by_admin  BIGINT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uq_coupons_code (code),

  CONSTRAINT fk_coupons_admin
    FOREIGN KEY (created_by_admin) REFERENCES Users(user_id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Order_Coupons (
  order_id          BIGINT NOT NULL,
  coupon_id         BIGINT NOT NULL,
  applied_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_id, coupon_id),

  CONSTRAINT fk_ordercoupons_order
    FOREIGN KEY (order_id) REFERENCES Orders(order_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_ordercoupons_coupon
    FOREIGN KEY (coupon_id) REFERENCES Coupons(coupon_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Wishlists (
  wishlist_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id       BIGINT NOT NULL,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY uq_wishlist_customer (customer_id),

  CONSTRAINT fk_wishlists_customer
    FOREIGN KEY (customer_id) REFERENCES Users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Wishlist_Items (
  wishlist_id       BIGINT NOT NULL,
  product_id        BIGINT NOT NULL,
  added_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (wishlist_id, product_id),

  CONSTRAINT fk_wishlistitems_wishlist
    FOREIGN KEY (wishlist_id) REFERENCES Wishlists(wishlist_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,

  CONSTRAINT fk_wishlistitems_product
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;


CREATE TABLE Notifications (
  notification_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id           BIGINT NOT NULL,
  title             VARCHAR(160) NOT NULL,
  message           TEXT NOT NULL,
  is_read           BOOLEAN NOT NULL DEFAULT FALSE,
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_notifications_user_read ON Notifications(user_id, is_read);

DELIMITER $$

CREATE TRIGGER trg_reviews_only_after_shipped
BEFORE INSERT ON Reviews
FOR EACH ROW
BEGIN
  DECLARE v_status VARCHAR(20);

  SELECT o.status
    INTO v_status
  FROM Orders o
  JOIN Order_Items oi ON oi.order_id = o.order_id
  WHERE oi.order_item_id = NEW.order_item_id
  LIMIT 1;

  IF v_status IS NULL OR (v_status NOT IN ('SHIPPED','DELIVERED')) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Review can only be submitted after the order is marked as SHIPPED or DELIVERED.';
  END IF;
END$$

CREATE TRIGGER trg_orderitems_check_stock
BEFORE INSERT ON Order_Items
FOR EACH ROW
BEGIN
  DECLARE v_stock INT;

  SELECT stock_quantity INTO v_stock
  FROM Products
  WHERE product_id = NEW.product_id
  FOR UPDATE;

  IF v_stock IS NULL OR v_stock < NEW.quantity THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Insufficient stock for the selected product.';
  END IF;
END$$

CREATE TRIGGER trg_orderitems_check_stock_update
BEFORE UPDATE ON Order_Items
FOR EACH ROW
BEGIN
  DECLARE v_stock INT;
  DECLARE v_delta INT;

  SET v_delta = NEW.quantity - OLD.quantity;

  IF v_delta > 0 THEN
    SELECT stock_quantity INTO v_stock
    FROM Products
    WHERE product_id = NEW.product_id
    FOR UPDATE;

    IF v_stock IS NULL OR v_stock < v_delta THEN
      SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient stock for increasing quantity.';
    END IF;
  END IF;
END$$

DELIMITER ;

