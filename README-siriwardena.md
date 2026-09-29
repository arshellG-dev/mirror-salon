# Member 4 — Siriwardena M.N.S. (IT25100606) — Orders, Payments & Reviews

## What this part does

Product ordering and checkout (cart, delivery fee by area, COD or bank
transfer), payment verification by staff, customer reviews of the salon, and
the revenue reports shown on the admin dashboard.

## Main classes

- **Entities**: `ProductOrder`, `OrderItem`, `Payment` (parent of
  `BankTransferPayment` and `CodPayment` via `@MapsId` class-table
  inheritance — one `PAYMENT` row per order, with method-specific extra
  columns split into their own tables), `Review`.
- **Repositories**: one per entity above.
- **Services**: `OrderCheckoutService` (builds an order from the cart,
  checks stock, computes the delivery fee), `PaymentVerificationService`
  (staff approving/rejecting a bank-transfer slip or collecting COD),
  `ReviewService`, `FileStorageService` (a generic local-disk upload helper —
  originally written for product images, later reused by Member 1 for
  stylist profile photos).
- **Controllers**: `CartController`, `CheckoutController`,
  `MyOrdersController`, `PaymentVerificationController`, `ReviewController`,
  `AdminReviewController`, `AdminRevenueController`.

## Key design decisions

- `Payment` uses class-table inheritance (`BankTransferPayment`/`CodPayment`
  each `@MapsId` to `Payment`) rather than a single wide `PAYMENT` table with
  nullable columns for both methods, so a COD payment never has a dangling
  `slip_file_path` column and vice versa.
- Stock is decremented only when a payment is **verified** (not at
  checkout/order-placement time), so an order stuck waiting for bank-transfer
  proof doesn't reserve stock indefinitely.
- `RevenueRepository` (an admin/reporting repository) was kept in this
  module because "revenue reports" is this member's page, even though the
  repository itself is the single most cross-cutting piece of SQL in the
  project — it reads `BOOKING` (Member 3), `PRODUCT_ORDER` (this module), and
  `DELIVERY_AREA` (Member 2) in native queries. See `DEPENDENCY_ANALYSIS.md`.

## Depends on other members

- Needs `catalogue.Product`, `ProductRepository`, `DeliveryArea`,
  `DeliveryAreaRepository` from **Member 2**.
- Needs `users.Customer`, `CustomerSession`, `Staff`, `StaffRepository`,
  `Notification`, `NotificationRepository` from **Member 1**.
- `RevenueRepository` additionally needs `booking.Booking` from **Member 3**
  (only that one file — the rest of this module has no Booking dependency).
- Does **not** depend on Booking for anything else — Orders and Booking are
  independent modules.

## Tests

`PaymentVerificationServiceTest.java` — covers `reject()`: a blank reason is
refused before any repository is touched, a valid rejection flips both the
`Payment` and its `ProductOrder` to their rejected states and queues exactly
one customer notification, and a payment that was already processed cannot
be rejected again. This logic had no test coverage before this exercise.
