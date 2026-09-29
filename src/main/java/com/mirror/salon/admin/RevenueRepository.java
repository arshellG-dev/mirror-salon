package com.mirror.salon.admin;

import com.mirror.salon.booking.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RevenueRepository extends JpaRepository<Booking, Integer> {

    @Query(nativeQuery = true, value = """
            SELECT DATE_FORMAT(b.booking_date, '%Y-%m') AS month,
                   sv.service_name AS serviceName,
                   CONCAT(s.first_name, ' ', s.last_name) AS stylistName,
                   SUM(bs.price_at_booking) AS revenue
            FROM BOOKING b
            JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
            JOIN SERVICE sv ON sv.service_id = bs.service_id
            JOIN STAFF s ON s.staff_id = b.stylist_id
            WHERE b.status = 'COMPLETED'
              AND (:from IS NULL OR b.booking_date >= :from)
              AND (:to IS NULL OR b.booking_date <= :to)
            GROUP BY month, sv.service_name, stylistName
            ORDER BY month DESC, revenue DESC
            """)
    List<ServiceRevenueRow> serviceRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(bs.price_at_booking), 0)
            FROM BOOKING b JOIN BOOKING_SERVICE bs ON bs.booking_id = b.booking_id
            WHERE b.status = 'COMPLETED'
              AND (:from IS NULL OR b.booking_date >= :from)
              AND (:to IS NULL OR b.booking_date <= :to)
            """)
    BigDecimal serviceRevenueTotal(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(nativeQuery = true, value = """
            SELECT DATE_FORMAT(o.order_date, '%Y-%m') AS month,
                   p.product_name AS productName,
                   SUM(oi.quantity * oi.unit_price) AS revenue
            FROM PRODUCT_ORDER o
            JOIN ORDER_ITEM oi ON oi.order_id = o.order_id
            JOIN PRODUCT p ON p.product_id = oi.product_id
            WHERE o.order_status IN ('CONFIRMED','DISPATCHED','DELIVERED')
              AND (:from IS NULL OR o.order_date >= :from)
              AND (:to IS NULL OR o.order_date < DATE_ADD(:to, INTERVAL 1 DAY))
            GROUP BY month, p.product_name
            ORDER BY month DESC, revenue DESC
            """)
    List<ProductRevenueRow> productRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(nativeQuery = true, value = """
            SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
            FROM PRODUCT_ORDER o JOIN ORDER_ITEM oi ON oi.order_id = o.order_id
            WHERE o.order_status IN ('CONFIRMED','DISPATCHED','DELIVERED')
              AND (:from IS NULL OR o.order_date >= :from)
              AND (:to IS NULL OR o.order_date < DATE_ADD(:to, INTERVAL 1 DAY))
            """)
    BigDecimal productRevenueTotal(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(nativeQuery = true, value = """
            SELECT a.area_name AS areaName, SUM(o.delivery_fee) AS revenue
            FROM PRODUCT_ORDER o JOIN DELIVERY_AREA a ON a.area_id = o.area_id
            WHERE o.order_status <> 'REJECTED'
              AND (:from IS NULL OR o.order_date >= :from)
              AND (:to IS NULL OR o.order_date < DATE_ADD(:to, INTERVAL 1 DAY))
            GROUP BY a.area_name
            ORDER BY revenue DESC
            """)
    List<DeliveryRevenueRow> deliveryRevenueByArea(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
