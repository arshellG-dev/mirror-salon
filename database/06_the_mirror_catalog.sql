-- ============================================================
-- The Mirror Salon — Real catalog reload
-- File: 06_the_mirror_catalog.sql
-- NOT part of the original provided schema/data scripts (00-05).
-- Run this AFTER 00-04 (and optionally 05) to replace the placeholder
-- demo catalog (4 categories / 7 services) with the salon's real menu:
-- 9 categories / ~50 bookable service rows, sourced from the UI design
-- handoff's data/catalog.json ("The Mirror — Makeover Place").
--
-- This does not change any table structure — only data. It clears out
-- the sample catalog and any bookings/orders/reviews that reference it
-- (via FK RESTRICT), then reloads real categories, services, products,
-- stylist qualifications and placeholder reviews.
--
-- Modelling note: the schema has only two catalog levels (SERVICE_CATEGORY
-- -> SERVICE). The design has three (Category -> Section -> Service, e.g.
-- Facials -> Sothys/Jovees/Face add-ons -> Cleanup/Basic Facial/...).
-- Rather than changing the schema, each SERVICE's `description` column is
-- reused to hold its section title, and insertion order (service_id) is
-- relied on to keep section rows contiguous. "Full / Half" price pairs
-- (waxing) are modelled as two separate, independently bookable SERVICE
-- rows — "X" and "X (Half)" — since the schema requires one fixed price
-- per service; the service-detail page pairs them back up for display.
-- ============================================================

USE mirror_salon;

SET FOREIGN_KEY_CHECKS = 0;

-- ---- Clear transactional data that references the old catalog ----
DELETE FROM BOOKING_SERVICE;
DELETE FROM OTP_VERIFICATION WHERE booking_id IS NOT NULL;
DELETE FROM NOTIFICATION WHERE booking_id IS NOT NULL OR order_id IS NOT NULL;
DELETE FROM BOOKING;
DELETE FROM ORDER_ITEM;
DELETE FROM BANK_TRANSFER_PAYMENT;
DELETE FROM COD_PAYMENT;
DELETE FROM PAYMENT;
DELETE FROM PRODUCT_ORDER;
DELETE FROM REVIEW;

-- ---- Clear the old catalog ----
DELETE FROM SERVICE_PRODUCT;
DELETE FROM STYLIST_SERVICE;
DELETE FROM SERVICE;
DELETE FROM SERVICE_CATEGORY;
DELETE FROM PRODUCT_IMAGE;
DELETE FROM PRODUCT;

ALTER TABLE SERVICE_CATEGORY AUTO_INCREMENT = 1;
ALTER TABLE SERVICE AUTO_INCREMENT = 1;
ALTER TABLE PRODUCT AUTO_INCREMENT = 1;
ALTER TABLE BOOKING AUTO_INCREMENT = 1;
ALTER TABLE PRODUCT_ORDER AUTO_INCREMENT = 1;
ALTER TABLE REVIEW AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SERVICE_CATEGORY (9) — order matches the design's home-page split:
-- categories 1-4 = "Face & Body", 5-9 = "Hair" (the app hardcodes that split).
-- ============================================================
INSERT INTO SERVICE_CATEGORY (category_id, category_name, description) VALUES
(1, 'Facials',        'Sothys and Jovees facials — from a quick cleanup to our illuminating ritual, with massage and bleaching add-ons.'),
(2, 'Threading',       'Precise, clean shaping for brows and face.'),
(3, 'Nails',           'Manicures, pedicures and colour, finished with care.'),
(4, 'Waxing',          'Smooth results with full and half options.'),
(5, 'Haircuts',        'Trims, short cuts and layers, shaped to you.'),
(6, 'Hair Setting',    'Blow-dries, ironing and curls for any occasion.'),
(7, 'Hair Color',      'Full colour, highlights and grey coverage.'),
(8, 'Hair Relaxing',   'Lasting smoothness for front sections or full hair.'),
(9, 'Hair Treatments', 'Oil, conditioning and keratin care to restore shine.');

-- ============================================================
-- SERVICE — description column doubles as the "section" title.
-- ============================================================

-- ---- Facials (category 1) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(1, 'Cleanup (Sothys)',              'Sothys',      8500.00, 30, FALSE, TRUE),
(1, 'Basic Facial (Sothys)',         'Sothys',     10000.00, 45, FALSE, TRUE),
(1, 'Hydra Facial',                  'Sothys',     17500.00, 60, FALSE, TRUE),
(1, 'Illuminating Facial',           'Sothys',     25000.00, 90, TRUE,  TRUE),
(1, 'Seasonal Treatments',           'Sothys',     17500.00, 60, FALSE, TRUE),
(1, 'Cleanup (Jovees)',              'Jovees',      4500.00, 30, FALSE, TRUE),
(1, 'Basic Facial (Jovees)',         'Jovees',      5500.00, 40, FALSE, TRUE),
(1, 'Face Massage',                  'Face add-ons', 2500.00, 20, FALSE, TRUE),
(1, 'Face Bleaching',                'Face add-ons', 2000.00, 15, FALSE, TRUE);

-- ---- Threading (category 2) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(2, 'Eyebrows',    'Threading', 300.00, 10, FALSE, TRUE),
(2, 'Upper Lip',   'Threading', 300.00, 10, FALSE, TRUE),
(2, 'Forehead',    'Threading', 100.00, 10, FALSE, TRUE),
(2, 'Chin',        'Threading', 200.00, 10, FALSE, TRUE),
(2, 'Sides',       'Threading', 400.00, 15, FALSE, TRUE),
(2, 'Full Face',   'Threading', 1300.00, 25, FALSE, TRUE);

-- ---- Nails (category 3) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(3, 'Manicure',      'Nails', 3000.00, 45, FALSE, TRUE),
(3, 'Pedicure',      'Nails', 3500.00, 60, FALSE, TRUE),
(3, 'Nail Color',    'Nails', 1000.00, 20, FALSE, TRUE),
(3, 'Gel Pedicure',  'Nails', 4500.00, 75, FALSE, TRUE);

-- ---- Waxing (category 4) — "Full & half" pairs, then "Single area" ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(4, 'Legs',             'Full & half', 3500.00, 30, FALSE, TRUE),
(4, 'Legs (Half)',      'Full & half', 2500.00, 15, FALSE, TRUE),
(4, 'Arms',             'Full & half', 3000.00, 20, FALSE, TRUE),
(4, 'Arms (Half)',      'Full & half', 2000.00, 10, FALSE, TRUE),
(4, 'Berzelian',        'Full & half', 4000.00, 20, FALSE, TRUE),
(4, 'Berzelian (Half)', 'Full & half', 3500.00, 10, FALSE, TRUE),
(4, 'Back',             'Full & half', 3500.00, 25, FALSE, TRUE),
(4, 'Back (Half)',      'Full & half', 2500.00, 15, FALSE, TRUE),
(4, 'Underarm',         'Single area', 1500.00, 10, FALSE, TRUE),
(4, 'Full Face (Wax)',  'Single area', 2000.00, 20, FALSE, TRUE),
(4, 'Stomach',          'Single area', 2500.00, 15, FALSE, TRUE),
(4, 'Neck Only',        'Single area', 1500.00, 10, FALSE, TRUE);

-- ---- Haircuts (category 5) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(5, 'Hair Trimming',                 'Haircuts', 3000.00, 30, FALSE, TRUE),
(5, 'Hair Trimming (Without Wash)',  'Haircuts', 1500.00, 20, FALSE, TRUE),
(5, 'Short Haircut',                 'Haircuts', 2000.00, 30, FALSE, TRUE),
(5, 'Layer Cut',                     'Haircuts', 3500.00, 45, TRUE,  TRUE);

-- ---- Hair Setting (category 6) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(6, 'Blow Drying',   'Hair setting', 3000.00, 45, FALSE, TRUE),
(6, 'Hair Ironing',  'Hair setting', 3000.00, 45, FALSE, TRUE),
(6, 'Hair Curling',  'Hair setting', 3000.00, 45, FALSE, TRUE);

-- ---- Hair Color (category 7) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(7, 'Full Hair Color',          'Hair color',    10000.00, 120, TRUE,  TRUE),
(7, 'Highlights',                'Hair color',    12000.00, 150, TRUE,  TRUE),
(7, 'Roots',                     'For grey hair',  3500.00,  60, FALSE, TRUE),
(7, 'Roots (Front Only)',        'For grey hair',  2500.00,  30, FALSE, TRUE),
(7, 'Color Application Only',    'For grey hair',  2000.00,  30, FALSE, TRUE);

-- ---- Hair Relaxing (category 8) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(8, 'Front Parts', 'Hair relaxing',  8000.00,  60, FALSE, TRUE),
(8, 'Full Hair',   'Hair relaxing', 12000.00, 120, TRUE,  TRUE);

-- ---- Hair Treatments (category 9) ----
INSERT INTO SERVICE (category_id, service_name, description, price, duration_mins, is_restricted, is_active) VALUES
(9, 'Oil Treatment (With Wash)',    'Hair treatments', 3000.00, 30, FALSE, TRUE),
(9, 'Oil Treatment (Without Wash)', 'Hair treatments', 2500.00, 20, FALSE, TRUE),
(9, 'Wash and Dry',                 'Hair treatments', 1500.00, 20, FALSE, TRUE),
(9, 'Keratin Treatment',            'Hair treatments', 25000.00,150, TRUE,  TRUE),
(9, 'Conditioner Treatment',        'Hair treatments', 3500.00, 45, FALSE, TRUE);

-- ============================================================
-- STYLIST_SERVICE — every stylist (2=Nadeesha, 3=Kasun, 4=Ishara) can
-- perform every unrestricted service; restricted (premium) services are
-- limited to specific stylists, per spec §2 item 16 / §4 trigger note.
-- ============================================================
INSERT INTO STYLIST_SERVICE (stylist_id, service_id, assigned_date)
SELECT s.staff_id, sv.service_id, CURDATE()
FROM (SELECT 2 AS staff_id UNION SELECT 3 UNION SELECT 4) s
CROSS JOIN SERVICE sv
WHERE sv.is_restricted = FALSE;

INSERT INTO STYLIST_SERVICE (stylist_id, service_id, assigned_date)
SELECT 2, service_id, CURDATE() FROM SERVICE WHERE service_name IN ('Illuminating Facial', 'Full Hair Color', 'Highlights');

INSERT INTO STYLIST_SERVICE (stylist_id, service_id, assigned_date)
SELECT 3, service_id, CURDATE() FROM SERVICE WHERE service_name IN ('Keratin Treatment', 'Layer Cut');

INSERT INTO STYLIST_SERVICE (stylist_id, service_id, assigned_date)
SELECT 4, service_id, CURDATE() FROM SERVICE WHERE service_name IN ('Layer Cut', 'Full Hair');

-- ============================================================
-- PRODUCT — same 4 categories (Shampoo/Conditioner/Styling/Treatment,
-- ids 1-4 from 02_catalogue_sathsara.sql), real placeholder names/prices
-- from the design's shop page.
-- ============================================================
INSERT INTO PRODUCT (category_id, product_name, brand, size, description, price, stock_qty, is_active) VALUES
(1, 'Gentle Daily Shampoo',   NULL, NULL, 'For everyday use.',            2800.00, 30, TRUE),
(1, 'Colour Care Shampoo',    NULL, NULL, 'Protects colour-treated hair.', 3400.00, 25, TRUE),
(1, 'Anti-Frizz Shampoo',     NULL, NULL, 'Smooths and controls frizz.',   3200.00, 25, TRUE),
(2, 'Silk Conditioner',       NULL, NULL, 'Softening daily conditioner.',  3000.00, 30, TRUE),
(2, 'Colour Lock Conditioner',NULL, NULL, 'Locks in colour vibrancy.',     3600.00, 20, TRUE),
(2, 'Leave-in Conditioner',   NULL, NULL, 'Lightweight, no rinse.',        3300.00, 20, TRUE),
(3, 'Heat Protect Spray',     NULL, NULL, 'Protects against heat styling.',2900.00, 30, TRUE),
(3, 'Smoothing Serum',        NULL, NULL, 'Tames flyaways, adds shine.',   3800.00, 20, TRUE),
(3, 'Texture Mousse',         NULL, NULL, 'Volume and hold.',              2600.00, 20, TRUE),
(4, 'Keratin Repair Mask',    NULL, NULL, 'Weekly deep repair mask.',      6500.00, 15, TRUE),
(4, 'Argan Hair Oil',         NULL, NULL, 'Nourishing finishing oil.',     4800.00, 20, TRUE),
(4, 'Scalp Tonic',            NULL, NULL, 'Soothes and refreshes scalp.',  4200.00, 15, TRUE);

-- ============================================================
-- REVIEW — placeholder client reviews from the design handoff, attached
-- to existing sample customers.
-- ============================================================
INSERT INTO REVIEW (customer_id, rating, comment, reviewed_at, is_hidden) VALUES
(1, 5, 'Placeholder review — replace with a real client quote about their facial experience at The Mirror.', NOW() - INTERVAL 20 DAY, FALSE),
(2, 5, 'Placeholder review — replace with a real client quote about their hair treatment and the result.', NOW() - INTERVAL 15 DAY, FALSE),
(3, 5, 'Placeholder review — replace with a real client quote about their colour appointment.', NOW() - INTERVAL 10 DAY, FALSE),
(5, 5, 'Placeholder review — replace with a real client quote about their nails appointment.', NOW() - INTERVAL 5 DAY, FALSE);

-- ============================================================
-- STAFF password fix — 01_users_desilva.sql ships placeholder (non-bcrypt)
-- password_hash values that cannot be verified by Spring Security's
-- BCryptPasswordEncoder, so staff/admin login always fails against the
-- provided seed data. Replace with a real bcrypt hash for "Passw0rd!"
-- (same login password for all 5 seeded staff/admin accounts, for demo use).
-- ============================================================
UPDATE STAFF SET password_hash = '$2a$10$YemK95B3mPfS2HxD0WfVjesL/qnic/wkDBtSuo4UAAHOa0hPZAJm.';
