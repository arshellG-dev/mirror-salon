# Member 2 — Sathsara S.T.D. (IT25102752) — Service & Product Catalogue

## What this part does

The salon's services (haircuts, waxing, etc.) and retail products: browsing,
category filtering, product-service usage links, delivery areas for product
orders, and which stylists are qualified to perform which services.

## Main classes

- **Entities**: `SalonService`, `ServiceCategory`, `Product`, `ProductCategory`,
  `ProductImage`, `StylistService` (a join entity: which `Stylist` can perform
  which `SalonService`), `DeliveryArea`.
- **Repositories**: one per entity above, plus `StylistServiceRepository`.
- **Services**: `ServiceCatalogService` (browsing, category summaries, and the
  Full/Half price-pairing logic used on service detail pages),
  `ProductCatalogService`, `CatalogueAdminService` (admin CRUD for both
  service and product categories), `StylistServiceManagementService` (a
  stylist toggling which services they perform).
- **Controllers**: `ServiceController`, `ProductController`,
  `StylistServicesController` (the stylist's own "My Services" page),
  `AdminCatalogueController`, `AdminCategoryController`,
  `AdminDeliveryAreaController`.

## Key design decisions

- A service that comes in "Full" and "Half" variants (e.g. waxing) is stored
  as **two separate rows** in `SERVICE` (e.g. "Underarm" and "Underarm
  (Half)"), not one row with two prices — this keeps `SERVICE.price` a single
  column and matches how each variant is booked independently. The pairing
  back into one price-card row is a presentation-layer concern, done by
  `ServiceCatalogService.getPriceSections()` matching on the name minus the
  " (Half)" suffix.
- `StylistService` is a catalogue-owned join entity (not owned by
  Users/Access) because its lifecycle is driven by the catalogue admin page
  ("which stylists can perform this service") as much as by the stylist's own
  page — it was placed here to match the original database-design
  assignment's table ownership.

## Depends on other members

- `StylistService.java`, `StylistServiceManagementService.java`,
  `StylistServicesController.java` need `users.Stylist` / `StylistRepository`
  (and `users.Staff`/`StaffRepository`) from **Member 1**.
- No dependency on Booking (Member 3) or Orders (Member 4) — this module can
  be merged right after Member 1.

## Tests

`ServiceCatalogServiceTest.java` — covers the Full/Half price-pairing
algorithm in `getPriceSections()`: a Full+Half pair with the same description
must collapse into one row with both prices, an unrelated service stands
alone, and a Half-only service (no matching Full) still renders correctly on
its own. This logic had no test coverage anywhere in the project before this
exercise.
