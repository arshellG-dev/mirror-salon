-- ============================================================
-- The Mirror Salon — Queries owned by DE SILVA I.M. (IT25100535)
-- Extracted from the shared database/05_queries.sql (Q1-Q6, Q18).
-- Run 00_create_database.sql and 01_users_desilva.sql first.
-- ============================================================

USE mirror_salon;

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
