-- ============================================================
-- The Mirror Salon — Database Scripts
-- Module: Service and Product Catalogue
-- Owner: Sathsara S.T.D. (IT25102752)
-- File: 02_catalogue_sathsara.sql
-- Tables: SERVICE_CATEGORY, SERVICE, PRODUCT_CATEGORY, PRODUCT,
--         PRODUCT_IMAGE, SERVICE_PRODUCT, STYLIST_SERVICE, DELIVERY_AREA
-- Requires: 01_users_desilva.sql to have been run first (STYLIST table)
-- ============================================================

USE mirror_salon;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS STYLIST_SERVICE;
DROP TABLE IF EXISTS SERVICE_PRODUCT;
DROP TABLE IF EXISTS PRODUCT_IMAGE;
DROP TABLE IF EXISTS PRODUCT;
DROP TABLE IF EXISTS PRODUCT_CATEGORY;
DROP TABLE IF EXISTS SERVICE;
DROP TABLE IF EXISTS SERVICE_CATEGORY;
DROP TABLE IF EXISTS DELIVERY_AREA;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- SERVICE_CATEGORY
-- ------------------------------------------------------------
CREATE TABLE SERVICE_CATEGORY (
    category_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(50)     NOT NULL UNIQUE,
    description     VARCHAR(255)    NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- SERVICE
-- ------------------------------------------------------------
CREATE TABLE SERVICE (
    service_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id     INT UNSIGNED    NOT NULL,
    service_name    VARCHAR(100)    NOT NULL UNIQUE,
    description     TEXT            NULL,
    price           DECIMAL(10,2)   NOT NULL,
    duration_mins   SMALLINT UNSIGNED NOT NULL,
    is_restricted   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_service_category FOREIGN KEY (category_id)
        REFERENCES SERVICE_CATEGORY(category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_service_price CHECK (price >= 0),
    CONSTRAINT chk_service_duration CHECK (duration_mins > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PRODUCT_CATEGORY
-- ------------------------------------------------------------
CREATE TABLE PRODUCT_CATEGORY (
    category_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_name   VARCHAR(50)     NOT NULL UNIQUE,
    description     VARCHAR(255)    NULL
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PRODUCT
-- ------------------------------------------------------------
CREATE TABLE PRODUCT (
    product_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    category_id     INT UNSIGNED    NOT NULL,
    product_name    VARCHAR(100)    NOT NULL,
    brand           VARCHAR(60)     NULL,
    size            VARCHAR(30)     NULL,
    description     TEXT            NULL,
    price           DECIMAL(10,2)   NOT NULL,
    stock_qty       INT UNSIGNED    NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES PRODUCT_CATEGORY(category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_product_price CHECK (price >= 0),
    CONSTRAINT uq_product_identity UNIQUE (product_name, brand, size)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- PRODUCT_IMAGE (multivalued attribute of PRODUCT)
-- ------------------------------------------------------------
CREATE TABLE PRODUCT_IMAGE (
    product_id      INT UNSIGNED    NOT NULL,
    image_url       VARCHAR(255)    NOT NULL,
    PRIMARY KEY (product_id, image_url),
    CONSTRAINT fk_productimage_product FOREIGN KEY (product_id)
        REFERENCES PRODUCT(product_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- SERVICE_PRODUCT (M:N — products used in a service)
-- ------------------------------------------------------------
CREATE TABLE SERVICE_PRODUCT (
    service_id      INT UNSIGNED    NOT NULL,
    product_id      INT UNSIGNED    NOT NULL,
    PRIMARY KEY (service_id, product_id),
    CONSTRAINT fk_serviceproduct_service FOREIGN KEY (service_id)
        REFERENCES SERVICE(service_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_serviceproduct_product FOREIGN KEY (product_id)
        REFERENCES PRODUCT(product_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STYLIST_SERVICE (M:N — which stylist performs which service)
-- ------------------------------------------------------------
CREATE TABLE STYLIST_SERVICE (
    stylist_id      INT UNSIGNED    NOT NULL,
    service_id      INT UNSIGNED    NOT NULL,
    assigned_date   DATE            NOT NULL DEFAULT (CURRENT_DATE),
    PRIMARY KEY (stylist_id, service_id),
    CONSTRAINT fk_stylistservice_stylist FOREIGN KEY (stylist_id)
        REFERENCES STYLIST(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_stylistservice_service FOREIGN KEY (service_id)
        REFERENCES SERVICE(service_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- DELIVERY_AREA
-- ------------------------------------------------------------
CREATE TABLE DELIVERY_AREA (
    area_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    area_name       VARCHAR(60)     NOT NULL UNIQUE,
    delivery_fee    DECIMAL(8,2)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_deliveryarea_fee CHECK (delivery_fee >= 0)
) ENGINE=InnoDB;

-- ============================================================
-- Sample data — Sathsara
-- ============================================================

INSERT INTO SERVICE_CATEGORY (category_name, description) VALUES
('Hair Cutting',   'Haircuts and trims for all hair types'),
('Hair Colouring', 'Full colour, highlights, balayage and toning'),
('Hair Treatments','Deep conditioning, keratin and repair treatments'),
('Bridal & Event', 'Bridal styling and special-occasion looks');

INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(1, 'Ladies Haircut',        'Wash, cut and blow-dry for any hair length.',                       2500.00, 60,  FALSE, TRUE),
(1, 'Gents Haircut',         'Classic or modern gents haircut with styling.',                      1500.00, 30,  FALSE, TRUE),
(2, 'Global Hair Colour',    'Full head single-process colour.',                                  6500.00, 120, FALSE, TRUE),
(2, 'Balayage',               'Hand-painted highlights for a natural sun-kissed look.',            12000.00,180, TRUE,  TRUE),
(3, 'Keratin Treatment',      'Smoothing treatment that reduces frizz for up to 3 months.',        15000.00,150, TRUE,  TRUE),
(3, 'Deep Conditioning',      'Intensive moisture treatment for dry or damaged hair.',              3500.00, 45,  FALSE, TRUE),
(4, 'Bridal Hair Styling',    'Full bridal hair styling with trial session.',                      18000.00,180, TRUE,  TRUE);

INSERT INTO PRODUCT_CATEGORY (category_name, description) VALUES
('Shampoo',    'Cleansing hair products'),
('Conditioner','Conditioning hair products'),
('Styling',    'Styling gels, sprays and creams'),
('Treatment',  'Hair masks and leave-in treatments');

INSERT INTO PRODUCT (category_id, product_name, brand, size, description, price, stock_qty, is_active) VALUES
(1, 'Argan Oil Shampoo',        "L'Oreal",  '250ml', 'Nourishing shampoo with argan oil for dry hair.',       1850.00, 40, TRUE),
(2, 'Argan Oil Conditioner',    "L'Oreal",  '250ml', 'Matching conditioner for the Argan Oil Shampoo.',       1950.00, 35, TRUE),
(1, 'Colour Protect Shampoo',   'Wella',    '300ml', 'Sulphate-free shampoo for colour-treated hair.',        2400.00, 25, TRUE),
(3, 'Heat Protect Spray',       'Tresemme', '200ml', 'Protects hair from heat styling up to 230°C.',          1650.00, 30, TRUE),
(4, 'Keratin Repair Mask',      'Wella',    '150ml', 'Weekly deep repair mask for damaged hair.',             2800.00, 4,  TRUE),
(3, 'Curl Defining Cream',      'Tresemme', '200ml', 'Lightweight cream for defined, frizz-free curls.',      1750.00, 20, TRUE);

INSERT INTO PRODUCT_IMAGE (product_id, image_url) VALUES
(1, '/images/products/argan-shampoo-1.jpg'),
(1, '/images/products/argan-shampoo-2.jpg'),
(2, '/images/products/argan-conditioner-1.jpg'),
(3, '/images/products/colour-protect-1.jpg'),
(4, '/images/products/heat-protect-1.jpg'),
(5, '/images/products/keratin-mask-1.jpg'),
(6, '/images/products/curl-cream-1.jpg');

-- Which products are used to perform which service
INSERT INTO SERVICE_PRODUCT (service_id, product_id) VALUES
(3, 3), -- Global Hair Colour uses Colour Protect Shampoo
(4, 3), -- Balayage uses Colour Protect Shampoo
(5, 5), -- Keratin Treatment uses Keratin Repair Mask
(6, 2), -- Deep Conditioning uses Argan Oil Conditioner
(6, 5), -- Deep Conditioning also uses Keratin Repair Mask
(7, 4); -- Bridal Hair Styling uses Heat Protect Spray

-- Which stylist performs which service (staff_id 2 = Nadeesha, 3 = Kasun, 4 = Ishara)
INSERT INTO STYLIST_SERVICE (stylist_id, service_id, assigned_date) VALUES
(2, 3, '2020-06-15'), -- Nadeesha: Global Hair Colour
(2, 4, '2020-06-15'), -- Nadeesha: Balayage (restricted)
(2, 6, '2020-06-15'), -- Nadeesha: Deep Conditioning
(3, 1, '2021-01-10'), -- Kasun: Ladies Haircut
(3, 2, '2021-01-10'), -- Kasun: Gents Haircut
(3, 6, '2021-01-10'), -- Kasun: Deep Conditioning
(3, 5, '2021-06-01'), -- Kasun: Keratin Treatment (restricted)
(4, 1, '2022-04-20'), -- Ishara: Ladies Haircut
(4, 7, '2022-04-20'); -- Ishara: Bridal Hair Styling (restricted)

INSERT INTO DELIVERY_AREA (area_name, delivery_fee, is_active) VALUES
('Mount Lavinia', 300.00, TRUE),
('Dehiwala',       350.00, TRUE),
('Colombo',        450.00, TRUE),
('Moratuwa',        400.00, TRUE),
('Other (Western Province)', 600.00, TRUE);
