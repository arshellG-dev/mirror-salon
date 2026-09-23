-- ============================================================
-- The Mirror Salon — Database Setup
-- SE2032 Database Management Systems
-- File: 00_create_database.sql
-- Run this FIRST, once, before any of the numbered member scripts.
-- ============================================================

CREATE DATABASE IF NOT EXISTS mirror_salon
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mirror_salon;

-- Each member script (01_*.sql, 02_*.sql, 03_*.sql, 04_*.sql) can then
-- be run in order. Every script starts with "USE mirror_salon;" so they
-- can also be run independently once this file has been run once.
