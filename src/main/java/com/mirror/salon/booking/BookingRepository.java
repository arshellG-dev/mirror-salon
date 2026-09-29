package com.mirror.salon.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByCustomer_CustomerIdOrderByBookingDateDesc(Integer customerId);

    List<Booking> findByStylist_StaffIdAndBookingDate(Integer stylistId, LocalDate bookingDate);

    List<Booking> findByStylist_StaffIdAndBookingDateBetween(Integer stylistId, LocalDate from, LocalDate to);

    List<Booking> findByStatusAndCreatedAtBefore(Booking.Status status, LocalDateTime cutoff);

    List<Booking> findByStatusAndBookingDate(Booking.Status status, LocalDate bookingDate);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.stylist st LEFT JOIN FETCH st.staff "
            + "WHERE b.customer.customerId = :customerId ORDER BY b.bookingDate DESC")
    List<Booking> findByCustomerIdWithDetails(@Param("customerId") Integer customerId);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.stylist st LEFT JOIN FETCH st.staff "
            + "LEFT JOIN FETCH b.customer "
            + "WHERE b.bookingId = :bookingId")
    Optional<Booking> findByIdWithDetails(@Param("bookingId") Integer bookingId);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.customer "
            + "WHERE b.stylist.staffId = :stylistId ORDER BY b.bookingDate DESC, b.startTime")
    List<Booking> findByStylistIdWithDetails(@Param("stylistId") Integer stylistId);

    long countByBookingDate(LocalDate bookingDate);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.stylist st LEFT JOIN FETCH st.staff "
            + "LEFT JOIN FETCH b.customer "
            + "WHERE b.bookingDate = :date ORDER BY b.startTime")
    List<Booking> findByBookingDateWithDetails(@Param("date") LocalDate date);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.stylist st LEFT JOIN FETCH st.staff "
            + "LEFT JOIN FETCH b.customer "
            + "LEFT JOIN FETCH b.createdByStaff "
            + "ORDER BY b.bookingDate DESC, b.startTime DESC")
    List<Booking> findAllWithDetails();

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.customer "
            + "WHERE b.stylist.staffId = :stylistId AND b.bookingDate = :date ORDER BY b.startTime")
    List<Booking> findByStylistIdAndDateWithDetails(@Param("stylistId") Integer stylistId, @Param("date") LocalDate date);

    long countByStylist_StaffIdAndBookingDateBetween(Integer stylistId, LocalDate from, LocalDate to);

    @Query("SELECT DISTINCT b FROM Booking b "
            + "LEFT JOIN FETCH b.services bs LEFT JOIN FETCH bs.service "
            + "LEFT JOIN FETCH b.customer "
            + "WHERE b.stylist.staffId = :stylistId AND b.bookingDate BETWEEN :from AND :to "
            + "ORDER BY b.bookingDate, b.startTime")
    List<Booking> findByStylistIdAndDateBetweenWithDetails(@Param("stylistId") Integer stylistId,
                                                            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
