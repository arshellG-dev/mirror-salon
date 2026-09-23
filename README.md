# The Mirror Salon — Database Scripts

## Run order

Run these in MySQL Workbench, in this exact order, on a fresh connection:

1. `00_create_database.sql` — creates the `mirror_salon` database
2. `01_users_desilva.sql` — Customer, Staff, Stylist, OTP, Notification
3. `02_catalogue_sathsara.sql` — Services, Products, categories, delivery areas
4. `03_booking_gomes.sql` — Bookings, working hours, unavailability, holidays
5. `04_orders_siriwardena.sql` — Orders, payments, reviews
6. `05_queries.sql` — all 28 SQL queries required by the organization (run only after 00–04)

Each of the four member scripts is self-contained (drops its own tables first with
`DROP TABLE IF EXISTS`), so re-running one on its own is safe once the earlier ones
have been run at least once. Running all five in order on an empty database builds
the whole schema plus sample data from scratch.

**Every teammate should run all five files, in order, on their own local MySQL**,
using the `salon_app` user set up earlier. This keeps everyone's local database
identical, which is important for the Spring Boot app: since `ddl-auto=validate`
is set, Spring checks its Java entity classes against these exact tables.

## Why the files reference each other

- `01_users_desilva.sql` creates `OTP_VERIFICATION.booking_id` and
  `NOTIFICATION.booking_id` / `NOTIFICATION.order_id` as plain nullable columns,
  without a foreign key yet — because `BOOKING` and `PRODUCT_ORDER` don't exist
  at that point in the run order.
- `03_booking_gomes.sql` completes those with `ALTER TABLE ... ADD CONSTRAINT`
  once `BOOKING` exists.
- `04_orders_siriwardena.sql` completes the last one (`NOTIFICATION.order_id`)
  once `PRODUCT_ORDER` exists.

This is a normal pattern for circular-looking references (Notification points at
both Booking and Order; Booking and Order don't point back at Notification) and
matches the mapping shown in the Part 2 report.

## Triggers

Two business rules that a plain CHECK constraint can't express are enforced with
triggers:

- **`trg_staff_single_admin_insert` / `_update`** (in 01): only one STAFF row can
  have `staff_type = 'ADMIN'`.
- **`trg_booking_no_overlap_insert` / `_update`** (in 03): a stylist cannot have
  two overlapping CONFIRMED/PENDING_OTP bookings, cannot be booked during a
  blocked `STYLIST_UNAVAILABILITY` period, and cannot be booked on a
  `SALON_HOLIDAY` date.

Try inserting a deliberately overlapping booking in Workbench to see the trigger
fire — this is worth demonstrating at the viva.

## Sample data

Each member script inserts a small but realistic set of rows for their own
tables, referencing the tables created earlier in the run order. Together the
five scripts populate:

- 8 customers, 5 staff (1 admin, 3 stylists, 1 receptionist)
- 4 service categories, 7 services · 4 product categories, 6 products
- 6 bookings across different statuses (confirmed, completed, pending OTP,
  no-show) · 3 product orders with items and payments (COD and bank transfer)
- 3 reviews, one with a reply

## Queries (05_queries.sql)

All 28 queries from Table 7 of the Part 2 report, grouped and commented by the
student responsible, matching the requirement of at least 4 queries per member.
Each is written as standard MySQL 8 syntax (`WITH RECURSIVE`, window functions,
`DATE_FORMAT`, `GROUP_CONCAT ... SEPARATOR`, `INTERVAL` arithmetic) — all
supported natively in MySQL 8, which is what Workbench and the Spring Boot
MySQL Driver both use.

For the report, take a screenshot of each query's result in Workbench and paste
it under the matching query in Table 7.
