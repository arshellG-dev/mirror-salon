-- ============================================================
-- The Mirror Salon — Database Scripts
-- Module: Users, Access and Notifications
-- Owner: De Silva I.M. (IT25100535)
-- File: 01_users_desilva.sql
-- Tables: CUSTOMER, STAFF, STYLIST, OTP_VERIFICATION, NOTIFICATION
-- ============================================================

USE mirror_salon;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS NOTIFICATION;
DROP TABLE IF EXISTS OTP_VERIFICATION;
DROP TABLE IF EXISTS STYLIST;
DROP TABLE IF EXISTS STAFF;
DROP TABLE IF EXISTS CUSTOMER;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- CUSTOMER
-- ------------------------------------------------------------
CREATE TABLE CUSTOMER (
    customer_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100)    NOT NULL,
    mobile_no       CHAR(10)        NOT NULL UNIQUE,
    is_verified     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_via     ENUM('ONLINE','WALK_IN','ADMIN') NOT NULL DEFAULT 'ONLINE',
    admin_notes     VARCHAR(500)    NULL,
    registered_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customer_mobile CHECK (mobile_no REGEXP '^0[0-9]{9}$')
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STAFF (superclass: ADMIN / STYLIST / RECEPTIONIST via staff_type)
-- ------------------------------------------------------------
CREATE TABLE STAFF (
    staff_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    mobile_no       CHAR(10)        NULL UNIQUE,
    username        VARCHAR(30)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    staff_type      ENUM('ADMIN','STYLIST','RECEPTIONIST') NOT NULL,
    joined_date     DATE            NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- STYLIST (subclass of STAFF)
-- ------------------------------------------------------------
CREATE TABLE STYLIST (
    staff_id        INT UNSIGNED PRIMARY KEY,
    bio             VARCHAR(500)    NULL,
    photo_url       VARCHAR(255)    NULL,
    specialty_title VARCHAR(100)    NULL,
    CONSTRAINT fk_stylist_staff FOREIGN KEY (staff_id)
        REFERENCES STAFF(staff_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- OTP_VERIFICATION
-- (booking_id FK is added later in 03_booking_gomes.sql, once
--  BOOKING exists, via ALTER TABLE)
-- ------------------------------------------------------------
CREATE TABLE OTP_VERIFICATION (
    otp_id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT UNSIGNED    NOT NULL,
    booking_id      INT UNSIGNED    NULL,
    purpose         ENUM('LOGIN','BOOKING_CONFIRMATION') NOT NULL,
    otp_hash        VARCHAR(255)    NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      DATETIME        NOT NULL,
    verified_at     DATETIME        NULL,
    attempt_count   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_otp_customer FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_otp_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_otp_attempts CHECK (attempt_count <= 5),
    CONSTRAINT chk_otp_purpose CHECK (
        (purpose = 'LOGIN' AND booking_id IS NULL) OR
        (purpose = 'BOOKING_CONFIRMATION' AND booking_id IS NOT NULL)
    )
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- NOTIFICATION
-- (booking_id / order_id FKs are added later by 03_booking_gomes.sql
--  and 04_orders_siriwardena.sql once those tables exist)
-- ------------------------------------------------------------
CREATE TABLE NOTIFICATION (
    notification_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT UNSIGNED    NOT NULL,
    booking_id      INT UNSIGNED    NULL,
    order_id        INT UNSIGNED    NULL,
    notif_type      ENUM('BOOKING_CONFIRMED','BOOKING_REMINDER','BOOKING_CANCELLED',
                          'BOOKING_RESCHEDULED','ORDER_PLACED','PAYMENT_REJECTED',
                          'ORDER_STATUS') NOT NULL,
    channel         ENUM('SMS')     NOT NULL DEFAULT 'SMS',
    message         VARCHAR(320)    NOT NULL,
    scheduled_at    DATETIME        NOT NULL,
    sent_at         DATETIME        NULL,
    send_status     ENUM('QUEUED','SENT','FAILED') NOT NULL DEFAULT 'QUEUED',
    CONSTRAINT fk_notification_customer FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER(customer_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_notification_target CHECK (booking_id IS NULL OR order_id IS NULL)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Trigger: only one ADMIN allowed (the owner)
-- ------------------------------------------------------------
DELIMITER $$
CREATE TRIGGER trg_staff_single_admin_insert
BEFORE INSERT ON STAFF
FOR EACH ROW
BEGIN
    IF NEW.staff_type = 'ADMIN' AND
       (SELECT COUNT(*) FROM STAFF WHERE staff_type = 'ADMIN') > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Only one ADMIN (the owner) is allowed in STAFF.';
    END IF;
END$$

CREATE TRIGGER trg_staff_single_admin_update
BEFORE UPDATE ON STAFF
FOR EACH ROW
BEGIN
    IF NEW.staff_type = 'ADMIN' AND OLD.staff_type <> 'ADMIN' AND
       (SELECT COUNT(*) FROM STAFF WHERE staff_type = 'ADMIN') > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Only one ADMIN (the owner) is allowed in STAFF.';
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- Sample data — De Silva
-- ============================================================

INSERT INTO STAFF (first_name, last_name, mobile_no, username, password_hash, staff_type, joined_date, is_active) VALUES
('Priyanka', 'Fernando', '0771234567', 'admin',      '$2a$10$examplehashADMIN0000000000000000000000000000',  'ADMIN',        '2019-03-01', TRUE),
('Nadeesha', 'Perera',   '0772345678', 'nadeesha.p', '$2a$10$examplehashSTYLIST100000000000000000000000', 'STYLIST',      '2020-06-15', TRUE),
('Kasun',    'Silva',    '0773456789', 'kasun.s',    '$2a$10$examplehashSTYLIST200000000000000000000000', 'STYLIST',      '2021-01-10', TRUE),
('Ishara',   'Jayawardena','0774567890','ishara.j',  '$2a$10$examplehashSTYLIST300000000000000000000000', 'STYLIST',      '2022-04-20', TRUE),
('Malsha',   'Gunawardena','0775678901','malsha.g',  '$2a$10$examplehashRECEPTION00000000000000000000000','RECEPTIONIST', '2021-11-05', TRUE);

INSERT INTO STYLIST (staff_id, bio, photo_url, specialty_title) VALUES
(2, 'Colour specialist with 6 years of experience in balayage and global colour.', '/images/staff/nadeesha.jpg', 'Colour Specialist'),
(3, 'All-round stylist skilled in cutting, styling and treatments.', '/images/staff/kasun.jpg', 'Senior Stylist'),
(4, 'Bridal and event styling specialist.', '/images/staff/ishara.jpg', 'Bridal Specialist');

INSERT INTO CUSTOMER (full_name, mobile_no, is_verified, created_via, admin_notes, registered_at) VALUES
('Amaya Rathnayake',   '0711112222', TRUE,  'ONLINE',  NULL,                              '2026-07-02 10:15:00'),
('Ruwan Wickramasinghe','0712223333', TRUE, 'ONLINE',  NULL,                              '2026-07-05 14:30:00'),
('Dilani Seneviratne',  '0713334444', TRUE, 'WALK_IN', NULL,                              '2026-07-08 09:00:00'),
('Chathura Bandara',    '0714445555', TRUE, 'ONLINE',  NULL,                              '2026-07-10 16:45:00'),
('Sanduni Peiris',      '0715556666', TRUE, 'ADMIN',   'Long-time customer, prefers Nadeesha.', '2025-01-15 00:00:00'),
('Hasitha Kumara',      '0716667777', TRUE, 'ONLINE',  NULL,                              '2026-08-01 11:20:00'),
('Tharushi Abeywardena','0717778888', FALSE,'ONLINE',  NULL,                              '2026-09-15 18:00:00'),
('Nimali Rajapaksha',   '0718889999', TRUE, 'ADMIN',   'Regular before the website launched.', '2024-11-20 00:00:00');

-- OTP samples: one used LOGIN otp (verified), one still-open BOOKING_CONFIRMATION otp
INSERT INTO OTP_VERIFICATION (customer_id, booking_id, purpose, otp_hash, created_at, expires_at, verified_at, attempt_count) VALUES
(1, NULL, 'LOGIN', SHA2('482913', 256), '2026-09-18 09:00:00', '2026-09-18 09:10:00', '2026-09-18 09:01:12', 1),
(2, NULL, 'LOGIN', SHA2('117364', 256), '2026-09-19 12:00:00', '2026-09-19 12:10:00', '2026-09-19 12:00:40', 1);

-- Notifications (booking_id/order_id left NULL here; illustrative rows,
-- fuller ones are inserted once BOOKING/PRODUCT_ORDER exist)
INSERT INTO NOTIFICATION (customer_id, booking_id, order_id, notif_type, channel, message, scheduled_at, sent_at, send_status) VALUES
(1, NULL, NULL, 'BOOKING_CONFIRMED', 'SMS', 'Your booking at The Mirror is confirmed for 22 Sep, 10:00 AM.', '2026-09-18 09:02:00', '2026-09-18 09:02:05', 'SENT'),
(2, NULL, NULL, 'BOOKING_REMINDER',  'SMS', 'Reminder: your appointment at The Mirror is tomorrow at 2:00 PM.', '2026-09-21 09:00:00', NULL, 'QUEUED');
