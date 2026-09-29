-- ============================================================
-- The Mirror Salon — SQL Queries Required by the Organization
-- File: 05_queries.sql
-- Run 00 through 04 first, then this file.
-- Matches Table 7 (2c) in the Part 2 report.
-- ============================================================

USE mirror_salon;


-- ============================================================
-- DE SILVA I.M. (IT25100535) — Users, Access, Notifications
-- ============================================================

-- Q1: Log a customer in — find by mobile number and check for a
-- valid LOGIN otp (not expired, not used, fewer than 5 attempts)
SELECT c.customer_id, c.full_name, o.otp_id, o.expires_at
FROM CUSTOMER c
JOIN OTP_VERIFICATION o ON o.customer_id = c.customer_id
WHERE c.mobile_no = '0711112222'
  AND o.purpose = 'LOGIN'
  AND o.verified_at IS NULL
  AND o.expires_at > NOW()
  AND o.attempt_count < 5
ORDER BY o.created_at DESC
LIMIT 1;

-- Q2: Customer database — every customer with first visit date,
-- completed visit count, and months since first visit
SELECT
    c.customer_id,
    c.full_name,
    c.mobile_no,
    MIN(b.booking_date)                                        AS first_visit_date,
    COUNT(CASE WHEN b.status = 'COMPLETED' THEN 1 END)          AS total_visits,
    TIMESTAMPDIFF(MONTH, MIN(b.booking_date), CURDATE())        AS months_as_customer
FROM CUSTOMER c
LEFT JOIN BOOKING b ON b.customer_id = c.customer_id AND b.status = 'COMPLETED'
GROUP BY c.customer_id, c.full_name, c.mobile_no
ORDER BY first_visit_date;

-- Q3: Full visit history of a selected customer
SELECT
    b.booking_id, b.booking_date, s.first_name AS stylist_first_name,
    s.last_name AS stylist_last_name, sv.service_name, bs.price_at_booking
FROM BOOKING b
JOIN STAFF s ON s.staff_id = b.stylist_id
JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
JOIN SERVICE sv ON sv.service_id = bs.service_id
WHERE b.customer_id = 1
ORDER BY b.booking_date DESC;

-- Q4: Customers who have not visited in the last 6 months
SELECT c.customer_id, c.full_name, c.mobile_no, MAX(b.booking_date) AS last_visit
FROM CUSTOMER c
JOIN BOOKING b ON b.customer_id = c.customer_id AND b.status = 'COMPLETED'
GROUP BY c.customer_id, c.full_name, c.mobile_no
HAVING MAX(b.booking_date) < DATE_SUB(CURDATE(), INTERVAL 6 MONTH);

-- Q5: 10 most frequent customers in a given period
SELECT c.customer_id, c.full_name, COUNT(*) AS visit_count
FROM CUSTOMER c
JOIN BOOKING b ON b.customer_id = c.customer_id
WHERE b.status = 'COMPLETED'
  AND b.booking_date BETWEEN '2026-01-01' AND '2026-12-31'
GROUP BY c.customer_id, c.full_name
ORDER BY visit_count DESC
LIMIT 10;

-- Q6: New customers registered each month, by how they were added
SELECT
    DATE_FORMAT(registered_at, '%Y-%m') AS month,
    created_via,
    COUNT(*)                             AS new_customers
FROM CUSTOMER
GROUP BY DATE_FORMAT(registered_at, '%Y-%m'), created_via
ORDER BY month, created_via;

-- Q18: Confirmed bookings for tomorrow that have not yet had a reminder sent
SELECT b.booking_id, c.full_name, c.mobile_no, b.booking_date, b.start_time
FROM BOOKING b
JOIN CUSTOMER c ON c.customer_id = b.customer_id
WHERE b.status = 'CONFIRMED'
  AND b.booking_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
  AND NOT EXISTS (
      SELECT 1 FROM NOTIFICATION n
      WHERE n.booking_id = b.booking_id AND n.notif_type = 'BOOKING_REMINDER'
  );


-- ============================================================
-- SATHSARA S.T.D. (IT25102752) — Service & Product Catalogue
-- ============================================================

-- Q7: All active services, optionally filtered by category
SELECT sv.service_id, sv.service_name, sc.category_name, sv.price, sv.duration_mins
FROM SERVICE sv
JOIN SERVICE_CATEGORY sc ON sc.category_id = sv.category_id
WHERE sv.is_active = TRUE
  AND (sc.category_name = 'Hair Colouring' OR 'Hair Colouring' IS NULL)
ORDER BY sc.category_name, sv.service_name;

-- Q8: Full details of one service, including products used and qualified stylists
SELECT sv.service_name, sc.category_name, sv.description, sv.price, sv.duration_mins,
       GROUP_CONCAT(DISTINCT p.product_name SEPARATOR ', ')                    AS products_used,
       GROUP_CONCAT(DISTINCT CONCAT(st.first_name,' ',st.last_name) SEPARATOR ', ') AS qualified_stylists
FROM SERVICE sv
JOIN SERVICE_CATEGORY sc ON sc.category_id = sv.category_id
LEFT JOIN SERVICE_PRODUCT sp ON sp.service_id = sv.service_id
LEFT JOIN PRODUCT p ON p.product_id = sp.product_id
LEFT JOIN STYLIST_SERVICE ss ON ss.service_id = sv.service_id
LEFT JOIN STAFF st ON st.staff_id = ss.stylist_id
WHERE sv.service_id = 4
GROUP BY sv.service_id, sv.service_name, sc.category_name, sv.description, sv.price, sv.duration_mins;

-- Q9: All active products with stock status, optionally filtered by category
SELECT p.product_id, p.product_name, p.brand, pc.category_name, p.price,
       CASE WHEN p.stock_qty > 0 THEN 'In stock' ELSE 'Out of stock' END AS stock_status
FROM PRODUCT p
JOIN PRODUCT_CATEGORY pc ON pc.category_id = p.category_id
WHERE p.is_active = TRUE
ORDER BY pc.category_name, p.product_name;

-- Q10: Full details of a product with images and the services that use it
SELECT p.product_name, p.brand, p.size, p.description, p.price,
       GROUP_CONCAT(DISTINCT pi.image_url SEPARATOR ', ')       AS images,
       GROUP_CONCAT(DISTINCT sv.service_name SEPARATOR ', ')    AS used_in_services
FROM PRODUCT p
LEFT JOIN PRODUCT_IMAGE pi ON pi.product_id = p.product_id
LEFT JOIN SERVICE_PRODUCT sp ON sp.product_id = p.product_id
LEFT JOIN SERVICE sv ON sv.service_id = sp.service_id
WHERE p.product_id = 5
GROUP BY p.product_id, p.product_name, p.brand, p.size, p.description, p.price;

-- Q11: Products with low stock (5 units or fewer)
SELECT product_id, product_name, brand, stock_qty
FROM PRODUCT
WHERE is_active = TRUE AND stock_qty <= 5
ORDER BY stock_qty;

-- Q12: Services a given stylist can perform (stylist's own Services tab)
SELECT sv.service_id, sv.service_name, sv.price, sv.duration_mins, ss.assigned_date
FROM STYLIST_SERVICE ss
JOIN SERVICE sv ON sv.service_id = ss.service_id
WHERE ss.stylist_id = 2
ORDER BY sv.service_name;


-- ============================================================
-- GOMES P.A.R. (IT25100950) — Booking & Appointment System
-- ============================================================

-- Q13: Stylists qualified to perform ALL services the customer selected
-- (example: services 3 and 6 selected together)
SELECT st.staff_id, s.first_name, s.last_name
FROM STYLIST st
JOIN STAFF s ON s.staff_id = st.staff_id
WHERE st.staff_id IN (
    SELECT stylist_id FROM STYLIST_SERVICE WHERE service_id IN (3, 6)
    GROUP BY stylist_id
    HAVING COUNT(DISTINCT service_id) = 2
);

-- Q14: Available 30-minute start times for a stylist on a chosen date
-- (example: stylist 2 on 2026-09-23; PT30M step generated via a recursive CTE)
-- Note: WEEKDAY() returns 0=Monday..6=Sunday, so +1 matches our
-- day_of_week convention (1=Monday..7=Sunday) used in STYLIST_WORKING_HOURS.
WITH RECURSIVE slots AS (
    SELECT wh.start_time AS slot_start
    FROM STYLIST_WORKING_HOURS wh
    WHERE wh.stylist_id = 2 AND wh.day_of_week = WEEKDAY('2026-09-23') + 1
    UNION ALL
    SELECT ADDTIME(slot_start, '00:30:00')
    FROM slots, STYLIST_WORKING_HOURS wh
    WHERE wh.stylist_id = 2 AND wh.day_of_week = WEEKDAY('2026-09-23') + 1
      AND ADDTIME(slot_start, '00:30:00') < wh.end_time
)
SELECT slot_start
FROM slots
WHERE NOT EXISTS (
        SELECT 1 FROM BOOKING b
        WHERE b.stylist_id = 2 AND b.booking_date = '2026-09-23'
          AND b.status IN ('CONFIRMED','PENDING_OTP')
          AND slot_start >= b.start_time AND slot_start < b.end_time
    )
  AND NOT EXISTS (
        SELECT 1 FROM STYLIST_UNAVAILABILITY su
        WHERE su.stylist_id = 2 AND su.block_date = '2026-09-23'
          AND slot_start >= su.start_time AND slot_start < su.end_time
    )
ORDER BY slot_start;

-- Q15: Auto-assign — from qualified, free stylists pick the one with
-- fewest bookings that day (example: service 1 on 2026-09-24)
SELECT st.staff_id, s.first_name, s.last_name, COUNT(b.booking_id) AS bookings_today
FROM STYLIST st
JOIN STAFF s ON s.staff_id = st.staff_id
JOIN STYLIST_SERVICE ss ON ss.stylist_id = st.staff_id AND ss.service_id = 1
LEFT JOIN BOOKING b ON b.stylist_id = st.staff_id
                    AND b.booking_date = '2026-09-24'
                    AND b.status IN ('CONFIRMED','PENDING_OTP')
GROUP BY st.staff_id, s.first_name, s.last_name
ORDER BY bookings_today ASC
LIMIT 1;

-- Q16: A customer's upcoming and past bookings with total price (My Bookings)
SELECT b.booking_id, b.booking_date, b.start_time, b.status,
       GROUP_CONCAT(sv.service_name SEPARATOR ', ') AS services,
       SUM(bs.price_at_booking)                     AS total_price
FROM BOOKING b
JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
JOIN SERVICE sv ON sv.service_id = bs.service_id
WHERE b.customer_id = 1
GROUP BY b.booking_id, b.booking_date, b.start_time, b.status
ORDER BY b.booking_date DESC;

-- Q17: A stylist's bookings for a chosen week (stylist dashboard)
SELECT b.booking_id, b.booking_date, b.start_time, b.end_time, b.status,
       c.full_name AS customer_name,
       GROUP_CONCAT(sv.service_name SEPARATOR ', ') AS services
FROM BOOKING b
JOIN CUSTOMER c ON c.customer_id = b.customer_id
JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
JOIN SERVICE sv ON sv.service_id = bs.service_id
WHERE b.stylist_id = 3
  AND b.booking_date BETWEEN '2026-09-21' AND '2026-09-27'
GROUP BY b.booking_id, b.booking_date, b.start_time, b.end_time, b.status, c.full_name
ORDER BY b.booking_date, b.start_time;

-- Q19: Expire bookings still waiting for OTP confirmation after 10 minutes
UPDATE BOOKING
SET status = 'EXPIRED'
WHERE status = 'PENDING_OTP'
  AND created_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE);


-- ============================================================
-- SIRIWARDENA M.N.S. (IT25100606) — Reviews, Orders & Admin
-- ============================================================

-- Q20: Bank-transfer orders waiting for slip verification, oldest first
SELECT o.order_id, c.full_name, bt.slip_file_path, bt.uploaded_at, p.amount
FROM PAYMENT p
JOIN BANK_TRANSFER_PAYMENT bt ON bt.payment_id = p.payment_id
JOIN PRODUCT_ORDER o ON o.order_id = p.order_id
JOIN CUSTOMER c ON c.customer_id = o.customer_id
WHERE p.payment_status = 'PENDING'
ORDER BY bt.uploaded_at;

-- Q21: Order summary — items, quantities, subtotal, delivery fee, grand total
SELECT o.order_id, pr.product_name, oi.quantity, oi.unit_price,
       (oi.quantity * oi.unit_price)                                         AS line_total,
       o.delivery_fee,
       SUM(oi.quantity * oi.unit_price) OVER (PARTITION BY o.order_id) + o.delivery_fee AS grand_total
FROM PRODUCT_ORDER o
JOIN ORDER_ITEM oi ON oi.order_id = o.order_id
JOIN PRODUCT pr ON pr.product_id = oi.product_id
WHERE o.order_id = 1;

-- Q22: Monthly service revenue from completed bookings, by service and stylist
SELECT
    DATE_FORMAT(b.booking_date, '%Y-%m') AS month,
    sv.service_name,
    CONCAT(s.first_name, ' ', s.last_name) AS stylist_name,
    SUM(bs.price_at_booking)               AS revenue
FROM BOOKING b
JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
JOIN SERVICE sv ON sv.service_id = bs.service_id
JOIN STAFF s ON s.staff_id = b.stylist_id
WHERE b.status = 'COMPLETED'
GROUP BY month, sv.service_name, stylist_name
ORDER BY month, revenue DESC;

-- Q23: Monthly product sales revenue by product
SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS month, p.product_name,
       SUM(oi.quantity * oi.unit_price)    AS revenue
FROM PRODUCT_ORDER o
JOIN ORDER_ITEM oi ON oi.order_id = o.order_id
JOIN PRODUCT p ON p.product_id = oi.product_id
WHERE o.order_status IN ('CONFIRMED','DISPATCHED','DELIVERED')
GROUP BY month, p.product_name
ORDER BY month, revenue DESC;

-- Q24: Most popular services by number of bookings in a period
SELECT sv.service_name, COUNT(*) AS times_booked
FROM BOOKING_SERVICE bs
JOIN SERVICE sv ON sv.service_id = bs.service_id
JOIN BOOKING b ON b.booking_id = bs.booking_id
WHERE b.booking_date BETWEEN '2026-01-01' AND '2026-12-31'
GROUP BY sv.service_name
ORDER BY times_booked DESC;

-- Q25: Bookings per stylist with completion, cancellation and no-show rates
SELECT
    CONCAT(s.first_name, ' ', s.last_name)                                       AS stylist_name,
    COUNT(*)                                                                     AS total_bookings,
    ROUND(100 * SUM(b.status = 'COMPLETED') / COUNT(*), 1)                       AS completed_pct,
    ROUND(100 * SUM(b.status = 'CANCELLED') / COUNT(*), 1)                       AS cancelled_pct,
    ROUND(100 * SUM(b.status = 'NO_SHOW')   / COUNT(*), 1)                       AS no_show_pct
FROM BOOKING b
JOIN STAFF s ON s.staff_id = b.stylist_id
GROUP BY stylist_name;

-- Q26: Average salon rating, review count, and visible reviews with replies
SELECT
    (SELECT ROUND(AVG(rating), 2) FROM REVIEW WHERE is_hidden = FALSE) AS average_rating,
    (SELECT COUNT(*) FROM REVIEW WHERE is_hidden = FALSE)              AS review_count;

SELECT r.review_id, c.full_name, r.rating, r.comment, r.reviewed_at, r.reply_text
FROM REVIEW r
JOIN CUSTOMER c ON c.customer_id = r.customer_id
WHERE r.is_hidden = FALSE
ORDER BY r.reviewed_at DESC;

-- Q27: Reviews that have not yet received a reply
SELECT r.review_id, c.full_name, r.rating, r.comment, r.reviewed_at
FROM REVIEW r
JOIN CUSTOMER c ON c.customer_id = r.customer_id
WHERE r.reply_text IS NULL AND r.is_hidden = FALSE
ORDER BY r.reviewed_at;

-- Q28: Number of orders and delivery revenue by delivery area
SELECT da.area_name, COUNT(o.order_id) AS order_count, SUM(o.delivery_fee) AS delivery_revenue
FROM DELIVERY_AREA da
LEFT JOIN PRODUCT_ORDER o ON o.area_id = da.area_id
GROUP BY da.area_name
ORDER BY delivery_revenue DESC;
