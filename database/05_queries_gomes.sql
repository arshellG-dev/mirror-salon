-- ============================================================
-- The Mirror Salon — Queries owned by GOMES P.A.R. (IT25100950)
-- Extracted from the shared database/05_queries.sql (Q13-Q19).
-- Run 00_create_database.sql and 03_booking_gomes.sql first.
-- ============================================================

USE mirror_salon;

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
