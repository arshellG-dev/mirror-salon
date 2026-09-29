# Member 1 — De Silva I.M. (IT25100535) — Users & Access

## What this part does

Everything about who is using the site: customer identity and OTP login,
staff/stylist accounts and authentication, and the notification log that
other modules write into. This is the schema's foundation — `CUSTOMER` and
`STAFF` are the parent tables that `BOOKING`, `PRODUCT_ORDER`, and `REVIEW`
all point back to via foreign keys.

## Main classes

- **Entities**: `Customer`, `Staff`, `Stylist` (a one-to-one specialisation of
  `Staff` for `staff_type = STYLIST`), `OtpVerification`, `Notification`.
- **Repositories**: `CustomerRepository`, `StaffRepository`,
  `StylistRepository`, `OtpVerificationRepository`, `NotificationRepository`.
- **Services**: `OtpService` (generates and verifies 6-digit SHA-256-hashed
  OTPs with a 10-minute expiry and 5-attempt lockout), `StaffManagementService`
  (enforces the "exactly one ADMIN account" rule), `StaffUserDetailsService`
  (Spring Security `UserDetailsService` for staff BCrypt login),
  `CustomerBackfillService` (a one-time admin job that reconstructs missing
  `Customer` rows from historical `Booking` data).
- **Controllers**: `CustomerAuthController` (mobile-number + OTP login),
  `StaffWebController` (staff login and dashboard routing by role),
  `StylistProfileController` (a stylist editing their own bio/photo),
  `AdminCustomerController`, `AdminStaffController`, `AdminNotificationController`.
- **Support**: `CustomerSession` (thin wrapper around the HTTP session for the
  logged-in customer id, used by every other module's controllers).

## Key design decisions

- Staff (including stylists) use **BCrypt** password hashing through Spring
  Security; customers never have a password — they authenticate with a
  mobile number plus a one-time OTP, hashed with SHA-256 (not BCrypt, since
  OTPs are single-use, minute-lived secrets, not long-lived credentials).
- `Stylist` is a class-table-inheritance child of `Staff` (`@MapsId`, shares
  its primary key) rather than a `staff_type` flag alone, so stylist-only
  attributes (bio, photo, specialty) don't pollute the `STAFF` table for
  admins/receptionists.
- Exactly one `ADMIN` staff account is enforced in `StaffManagementService`,
  not at the database level, since MariaDB has no portable "at most one row
  matching X" constraint.

## Depends on other members

This module is what the other three depend on (`Customer`, `Staff`,
`Stylist` are imported everywhere). In the other direction, five files in
this module reach back into other members and cannot compile until those
members' code exists — see `DEPENDENCY_ANALYSIS.md` at the repo root for the
full explanation and `COMMIT_PLAN.md` for the "Wave A / Wave B" commit split
this requires:

- `StaffWebController.java` → needs `booking.BookingRepository`
- `StylistProfileController.java` → needs `orders.FileStorageService`
- `CustomerBackfillService.java` → needs `booking.Booking/BookingRepository/BookingService`, `catalogue.SalonService/SalonServiceRepository`
- `CustomerRepository.java` → needs `booking.Booking.Status` (a fully-qualified inline reference in one `@Query` method — only found by actually compiling, not by scanning imports)
- `AdminCustomerController.java` → needs `booking.BookingRepository`, `catalogue.SalonServiceRepository`
- `AdminStaffController.java` → needs `booking.StylistScheduleService/StylistWorkingHours`, `catalogue.SalonServiceRepository/StylistServiceRepository`, `orders.FileStorageService`

## Tests

`OtpServiceTest.java` — covers OTP generation/verification: correct code
succeeds, wrong code increments the attempt counter and fails, an expired
record is rejected, and a customer with no OTP on file is rejected. This is
the only business logic in the whole project (as of this split) that had any
test coverage before this exercise — the rest of the project's `src/test`
was empty.
