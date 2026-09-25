-- ============================================================
-- The Mirror Salon — Database Scripts
-- Module: Reviews, Orders and Staff Management
-- Owner: Siriwardena M.N.S. (IT25100606)
-- File: 04_orders_siriwardena.sql
-- Tables: PRODUCT_ORDER, ORDER_ITEM, PAYMENT,
--         BANK_TRANSFER_PAYMENT, COD_PAYMENT, REVIEW
-- Requires: 01_users_desilva.sql, 02_catalogue_sathsara.sql and
--           03_booking_gomes.sql to have been run first.
-- ============================================================

USE mirror_salon;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS COD_PAYMENT;
DROP TABLE IF EXISTS BANK_TRANSFER_PAYMENT;
DROP TABLE IF EXISTS PAYMENT;
DROP TABLE IF EXISTS ORDER_ITEM;
DROP TABLE IF EXISTS PRODUCT_ORDER;
DROP TABLE IF EXISTS REVIEW;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- PRODUCT_ORDER
-- ------------------------------------------------------------
CREATE TABLE PRODUCT_ORDER (
    order_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT UNSIGNED    NOT NULL,
    area_id         INT UNSIGNED    NOT NULL,
    order_date      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recipient_name  VARCHAR(100)    NOT NULL,
    contact_no      CHAR(10)        NOT NULL,
    address_line    VARCHAR(200)    NOT NULL,
    city            VARCHAR(60)     NOT NULL,
    delivery_fee    DECIMAL(8,2)    NOT NULL,
    order_status    ENUM('PLACED','PAYMENT_PENDING','CONFIRMED','DISPATCHED','DELIVERED','REJECTED')
                                    NOT NULL DEFAULT 'PLACED',
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_order_area FOREIGN KEY (area_id)
        REFERENCES DELIVERY_AREA(area_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_order_contact CHECK (contact_no REGEXP '^0[0-9]{9}$'),
    CONSTRAINT chk_order_fee CHECK (delivery_fee >= 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- ORDER_ITEM (weak entity, owner: PRODUCT_ORDER)
-- ------------------------------------------------------------
CREATE TABLE ORDER_ITEM (
    order_id        INT UNSIGNED    NOT NULL,
    line_no         TINYINT UNSIGNED NOT NULL,
    product_id      INT UNSIGNED    NOT NULL,
    quantity        SMALLINT UNSIGNED NOT NULL,
    unit_price      DECIMAL(10,2)   NOT NULL,
    PRIMARY KEY (order_id, line_no),
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id)
        REFERENCES PRODUCT_ORDER(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id)
        REFERENCES PRODUCT(product_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_orderitem_qty CHECK (quantity > 0),
    CONSTRAINT chk_orderitem_price CHECK (unit_price >= 0),
    CONSTRAINT uq_orderitem_product UNIQUE (order_id, product_id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PAYMENT (superclass: BANK_TRANSFER_PAYMENT / COD_PAYMENT via payment_method)
-- ------------------------------------------------------------
CREATE TABLE PAYMENT (
    payment_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id          INT UNSIGNED    NOT NULL UNIQUE,
    payment_method     ENUM('COD','BANK_TRANSFER') NOT NULL,
    amount             DECIMAL(10,2)   NOT NULL,
    payment_status      ENUM('PENDING','VERIFIED','REJECTED','COLLECTED') NOT NULL DEFAULT 'PENDING',
    verified_by          INT UNSIGNED    NULL,
    verified_at           DATETIME        NULL,
    rejection_reason       VARCHAR(255)    NULL,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES PRODUCT_ORDER(order_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_payment_verifier FOREIGN KEY (verified_by)
        REFERENCES STAFF(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_verified CHECK (
        (verified_by IS NULL AND verified_at IS NULL) OR
        (verified_by IS NOT NULL AND verified_at IS NOT NULL)
    ),
    CONSTRAINT chk_payment_rejected CHECK (
        payment_status <> 'REJECTED' OR rejection_reason IS NOT NULL
    )
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- BANK_TRANSFER_PAYMENT (subclass of PAYMENT)
-- ------------------------------------------------------------
CREATE TABLE BANK_TRANSFER_PAYMENT (
    payment_id       INT UNSIGNED PRIMARY KEY,
    slip_file_path     VARCHAR(255)    NOT NULL,
    bank_reference       VARCHAR(50)     NULL,
    uploaded_at            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_banktransfer_payment FOREIGN KEY (payment_id)
        REFERENCES PAYMENT(payment_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- COD_PAYMENT (subclass of PAYMENT)
-- ------------------------------------------------------------
CREATE TABLE COD_PAYMENT (
    payment_id      INT UNSIGNED PRIMARY KEY,
    collected_at    DATETIME        NULL,
    CONSTRAINT fk_codpayment_payment FOREIGN KEY (payment_id)
        REFERENCES PAYMENT(payment_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- REVIEW
-- ------------------------------------------------------------
CREATE TABLE REVIEW (
    review_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT UNSIGNED    NOT NULL,
    rating          TINYINT UNSIGNED NOT NULL,
    comment         VARCHAR(1000)   NULL,
    reviewed_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_hidden       BOOLEAN         NOT NULL DEFAULT FALSE,
    reply_text      VARCHAR(1000)   NULL,
    replied_at      DATETIME        NULL,
    moderated_by    INT UNSIGNED    NULL,
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_review_moderator FOREIGN KEY (moderated_by)
        REFERENCES STAFF(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_reply CHECK (
        (reply_text IS NULL AND replied_at IS NULL) OR
        (reply_text IS NOT NULL AND replied_at IS NOT NULL)
    )
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Complete the forward reference from 01_users_desilva.sql
-- ------------------------------------------------------------
ALTER TABLE NOTIFICATION
    ADD CONSTRAINT fk_notification_order FOREIGN KEY (order_id)
        REFERENCES PRODUCT_ORDER(order_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- ============================================================
-- Sample data — Siriwardena
-- ============================================================

INSERT INTO PRODUCT_ORDER (customer_id, area_id, order_date, recipient_name, contact_no, address_line, city, delivery_fee, order_status) VALUES
(1, 1, '2026-09-15 14:20:00', 'Amaya Rathnayake',    '0711112222', '12 Galle Road',        'Mount Lavinia', 300.00, 'DELIVERED'),
(2, 3, '2026-09-18 09:10:00', 'Ruwan Wickramasinghe','0712223333', '45 Duplication Road',  'Colombo',       450.00, 'CONFIRMED'),
(4, 2, '2026-09-20 16:00:00', 'Chathura Bandara',    '0714445555', '8 Station Road',       'Dehiwala',      350.00, 'PAYMENT_PENDING');

INSERT INTO ORDER_ITEM (order_id, line_no, product_id, quantity, unit_price) VALUES
(1, 1, 1, 2, 1850.00),
(1, 2, 2, 1, 1950.00),
(2, 1, 4, 1, 1650.00),
(3, 1, 5, 1, 2800.00),
(3, 2, 6, 2, 1750.00);

INSERT INTO PAYMENT (order_id, payment_method, amount, payment_status, verified_by, verified_at, rejection_reason) VALUES
(1, 'COD',           5650.00, 'COLLECTED', 5, '2026-09-16 10:00:00', NULL),
(2, 'BANK_TRANSFER',  2100.00, 'VERIFIED',   5, '2026-09-18 11:30:00', NULL),
(3, 'BANK_TRANSFER',  6650.00, 'PENDING',    NULL, NULL,                 NULL);

INSERT INTO COD_PAYMENT (payment_id, collected_at) VALUES
(1, '2026-09-16 10:00:00');

INSERT INTO BANK_TRANSFER_PAYMENT (payment_id, slip_file_path, bank_reference, uploaded_at) VALUES
(2, '/uploads/slips/order2_slip.jpg', 'BOC-8842910', '2026-09-18 09:15:00'),
(3, '/uploads/slips/order3_slip.jpg', 'COMB-3391027','2026-09-20 16:05:00');

INSERT INTO REVIEW (customer_id, rating, comment, reviewed_at, is_hidden, reply_text, replied_at, moderated_by) VALUES
(5, 5, 'Amazing service every time, Nadeesha is fantastic with colour!', '2026-08-16 12:00:00', FALSE, 'Thank you so much for your kind words, Sanduni!', '2026-08-16 15:00:00', 1),
(1, 4, 'Great haircut, will come again.', '2026-08-20 10:00:00', FALSE, NULL, NULL, NULL),
(3, 5, 'Very friendly staff and clean salon.', '2026-09-01 09:00:00', FALSE, NULL, NULL, NULL);
