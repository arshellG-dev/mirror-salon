-- ============================================================
-- The Mirror Salon — Database Scripts
-- Module: Booking and Appointment System
-- Owner: Gomes P.A.R. (IT25100950)
-- File: 03_booking_gomes.sql
-- Tables: BOOKING, BOOKING_SERVICE, STYLIST_WORKING_HOURS,
--         STYLIST_UNAVAILABILITY, SALON_HOLIDAY
-- Requires: 01_users_desilva.sql and 02_catalogue_sathsara.sql
--           to have been run first.
-- ============================================================

USE mirror_salon;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS BOOKING_SERVICE;
DROP TABLE IF EXISTS BOOKING;
DROP TABLE IF EXISTS STYLIST_WORKING_HOURS;
DROP TABLE IF EXISTS STYLIST_UNAVAILABILITY;
DROP TABLE IF EXISTS SALON_HOLIDAY;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- STYLIST_WORKING_HOURS (weak entity, owner: STYLIST)
-- ------------------------------------------------------------
CREATE TABLE STYLIST_WORKING_HOURS (
    stylist_id      INT UNSIGNED    NOT NULL,
    day_of_week     TINYINT UNSIGNED NOT NULL,  -- 1=Monday ... 7=Sunday
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    PRIMARY KEY (stylist_id, day_of_week),
    CONSTRAINT fk_workinghours_stylist FOREIGN KEY (stylist_id)
        REFERENCES STYLIST(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_workinghours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_workinghours_order CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STYLIST_UNAVAILABILITY (weak entity, owner: STYLIST)
-- ------------------------------------------------------------
CREATE TABLE STYLIST_UNAVAILABILITY (
    stylist_id      INT UNSIGNED    NOT NULL,
    block_date      DATE            NOT NULL,
    start_time      TIME            NOT NULL,
    end_time        TIME            NOT NULL,
    reason          VARCHAR(150)    NULL,
    PRIMARY KEY (stylist_id, block_date, start_time),
    CONSTRAINT fk_unavailability_stylist FOREIGN KEY (stylist_id)
        REFERENCES STYLIST(staff_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_unavailability_order CHECK (end_time > start_time)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- SALON_HOLIDAY
-- ------------------------------------------------------------
CREATE TABLE SALON_HOLIDAY (
    holiday_date    DATE            PRIMARY KEY,
    description     VARCHAR(100)    NOT NULL,
    declared_by     INT UNSIGNED    NOT NULL,
    CONSTRAINT fk_holiday_admin FOREIGN KEY (declared_by)
        REFERENCES STAFF(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- BOOKING
-- ------------------------------------------------------------
CREATE TABLE BOOKING (
    booking_id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id         INT UNSIGNED NOT NULL,
    stylist_id          INT UNSIGNED NOT NULL,
    created_by_staff_id INT UNSIGNED NULL,
    booking_date        DATE         NOT NULL,
    start_time          TIME         NOT NULL,
    end_time            TIME         NOT NULL,
    status              ENUM('PENDING_OTP','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW','EXPIRED')
                                     NOT NULL DEFAULT 'PENDING_OTP',
    source              ENUM('ONLINE','WALK_IN','HISTORICAL') NOT NULL DEFAULT 'ONLINE',
    is_auto_assigned    BOOLEAN      NOT NULL DEFAULT FALSE,
    reschedule_count    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at         DATETIME     NULL,
    cancelled_by         ENUM('CUSTOMER','STYLIST','ADMIN') NULL,
    cancel_reason        VARCHAR(255) NULL,
    CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_booking_stylist FOREIGN KEY (stylist_id)
        REFERENCES STYLIST(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_booking_createdby FOREIGN KEY (created_by_staff_id)
        REFERENCES STAFF(staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_booking_time_order CHECK (end_time > start_time),
    CONSTRAINT chk_booking_slot CHECK (
        (MINUTE(start_time) = 0 OR MINUTE(start_time) = 30) AND SECOND(start_time) = 0
    ),
    CONSTRAINT chk_booking_source CHECK (
        (source = 'ONLINE' AND created_by_staff_id IS NULL) OR
        (source <> 'ONLINE' AND created_by_staff_id IS NOT NULL)
    ),
    CONSTRAINT chk_booking_cancel CHECK (
        status <> 'CANCELLED' OR (cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
    )
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- BOOKING_SERVICE (M:N — services included in a booking)
-- ------------------------------------------------------------
CREATE TABLE BOOKING_SERVICE (
    booking_id           INT UNSIGNED   NOT NULL,
    service_id            INT UNSIGNED   NOT NULL,
    price_at_booking       DECIMAL(10,2)  NOT NULL,
    duration_at_booking     SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (booking_id, service_id),
    CONSTRAINT fk_bookingservice_booking FOREIGN KEY (booking_id)
        REFERENCES BOOKING(booking_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bookingservice_service FOREIGN KEY (service_id)
        REFERENCES SERVICE(service_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_bookingservice_price CHECK (price_at_booking >= 0),
    CONSTRAINT chk_bookingservice_duration CHECK (duration_at_booking > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Complete the forward references from earlier scripts
-- ------------------------------------------------------------
ALTER TABLE OTP_VERIFICATION
    ADD CONSTRAINT fk_otp_booking FOREIGN KEY (booking_id)
        REFERENCES BOOKING(booking_id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE NOTIFICATION
    ADD CONSTRAINT fk_notification_booking FOREIGN KEY (booking_id)
        REFERENCES BOOKING(booking_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- ------------------------------------------------------------
-- Trigger: prevent overlapping bookings for the same stylist,
-- and block bookings on salon holidays.
-- (Applied on INSERT; a matching BEFORE UPDATE trigger covers
--  reschedules.)
-- ------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_booking_no_overlap_insert
BEFORE INSERT ON BOOKING
FOR EACH ROW
BEGIN
    IF NEW.status IN ('CONFIRMED','PENDING_OTP') THEN
        IF EXISTS (
            SELECT 1 FROM SALON_HOLIDAY WHERE holiday_date = NEW.booking_date
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot book on a salon holiday.';
        END IF;

        IF EXISTS (
            SELECT 1 FROM STYLIST_UNAVAILABILITY su
            WHERE su.stylist_id = NEW.stylist_id
              AND su.block_date = NEW.booking_date
              AND NEW.start_time < su.end_time
              AND NEW.end_time   > su.start_time
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stylist is unavailable at this time.';
        END IF;

        IF EXISTS (
            SELECT 1 FROM BOOKING b
            WHERE b.stylist_id = NEW.stylist_id
              AND b.booking_date = NEW.booking_date
              AND b.status IN ('CONFIRMED','PENDING_OTP')
              AND NEW.start_time < b.end_time
              AND NEW.end_time   > b.start_time
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This stylist already has a booking that overlaps this time.';
        END IF;
    END IF;
END$$

CREATE TRIGGER trg_booking_no_overlap_update
BEFORE UPDATE ON BOOKING
FOR EACH ROW
BEGIN
    IF NEW.status IN ('CONFIRMED','PENDING_OTP') THEN
        IF EXISTS (
            SELECT 1 FROM SALON_HOLIDAY WHERE holiday_date = NEW.booking_date
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot book on a salon holiday.';
        END IF;

        IF EXISTS (
            SELECT 1 FROM STYLIST_UNAVAILABILITY su
            WHERE su.stylist_id = NEW.stylist_id
              AND su.block_date = NEW.booking_date
              AND NEW.start_time < su.end_time
              AND NEW.end_time   > su.start_time
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stylist is unavailable at this time.';
        END IF;

        IF EXISTS (
            SELECT 1 FROM BOOKING b
            WHERE b.stylist_id = NEW.stylist_id
              AND b.booking_date = NEW.booking_date
              AND b.status IN ('CONFIRMED','PENDING_OTP')
              AND b.booking_id <> NEW.booking_id
              AND NEW.start_time < b.end_time
              AND NEW.end_time   > b.start_time
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'This stylist already has a booking that overlaps this time.';
        END IF;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- Sample data — Gomes
-- ============================================================

-- Regular weekly working hours (staff_id 2 = Nadeesha, 3 = Kasun, 4 = Ishara)
-- Salon is open Tue–Sun, closed Mondays. day_of_week: 1=Mon ... 7=Sun
INSERT INTO STYLIST_WORKING_HOURS (stylist_id, day_of_week, start_time, end_time) VALUES
(2, 2, '09:00:00', '17:00:00'), (2, 3, '09:00:00', '17:00:00'), (2, 4, '09:00:00', '17:00:00'),
(2, 5, '09:00:00', '17:00:00'), (2, 6, '09:00:00', '19:00:00'), (2, 7, '09:00:00', '19:00:00'),
(3, 2, '10:00:00', '18:00:00'), (3, 3, '10:00:00', '18:00:00'), (3, 4, '10:00:00', '18:00:00'),
(3, 5, '10:00:00', '18:00:00'), (3, 6, '09:00:00', '19:00:00'), (3, 7, '09:00:00', '19:00:00'),
(4, 4, '09:00:00', '17:00:00'), (4, 5, '09:00:00', '17:00:00'),
(4, 6, '09:00:00', '19:00:00'), (4, 7, '09:00:00', '19:00:00');

INSERT INTO STYLIST_UNAVAILABILITY (stylist_id, block_date, start_time, end_time, reason) VALUES
(3, '2026-09-25', '00:00:00', '23:59:59', 'Annual leave'),
(2, '2026-09-23', '13:00:00', '15:00:00', 'Personal appointment');

INSERT INTO SALON_HOLIDAY (holiday_date, description, declared_by) VALUES
('2026-10-26', 'Vap Poya Day', 1),
('2026-12-25', 'Christmas Day', 1);

-- Sample bookings
INSERT INTO BOOKING (customer_id, stylist_id, created_by_staff_id, booking_date, start_time, end_time,
                      status, source, is_auto_assigned, reschedule_count, created_at) VALUES
(1, 2, NULL, '2026-09-22', '10:00:00', '12:00:00', 'CONFIRMED', 'ONLINE',  FALSE, 0, '2026-09-18 09:00:00'),
(2, 3, NULL, '2026-09-22', '14:00:00', '14:30:00', 'CONFIRMED', 'ONLINE',  TRUE,  0, '2026-09-19 12:00:00'),
(3, 3, 5,    '2026-09-20', '11:00:00', '11:30:00', 'COMPLETED', 'WALK_IN', FALSE, 0, '2026-09-20 10:50:00'),
(4, 4, NULL, '2026-09-24', '15:00:00', '18:00:00', 'PENDING_OTP','ONLINE', FALSE, 0, '2026-09-21 12:04:00'),
(5, 2, NULL, '2026-08-15', '09:00:00', '11:00:00', 'COMPLETED', 'ONLINE',  FALSE, 0, '2026-08-10 09:00:00'),
(6, 3, NULL, '2026-09-10', '10:00:00', '10:30:00', 'NO_SHOW',   'ONLINE',  TRUE,  0, '2026-09-08 08:00:00');

INSERT INTO BOOKING_SERVICE (booking_id, service_id, price_at_booking, duration_at_booking) VALUES
(1, 3, 6500.00, 120),                  -- Amaya: Global Hair Colour with Nadeesha
(2, 2, 1500.00, 30),                   -- Ruwan: Gents Haircut with Kasun (auto-assigned)
(3, 1, 2500.00, 30),                   -- Dilani: walk-in Ladies Haircut
(4, 7, 18000.00, 180),                 -- Chathura: Bridal Hair Styling (note: sample only, normally a female client)
(5, 3, 6500.00, 120),                  -- Sanduni: Global Hair Colour (completed, past visit)
(6, 1, 2500.00, 30);                   -- Hasitha: Ladies Haircut (no-show)
