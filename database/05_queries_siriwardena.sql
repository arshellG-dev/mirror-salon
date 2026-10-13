-- ============================================================
-- The Mirror Salon — Queries owned by SIRIWARDENA M.N.S. (IT25100606)
-- Extracted from the shared database/05_queries.sql (Q20-Q28).
-- Run 00_create_database.sql and 04_orders_siriwardena.sql first.
-- ============================================================

USE mirror_salon;

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
