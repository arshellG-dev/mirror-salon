package com.mirror.salon.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByMobileNo(String mobileNo);

    @Query("SELECT new com.mirror.salon.users.CustomerStats(c.customerId, c.fullName, c.mobileNo, c.adminNotes, "
            + "MIN(CASE WHEN b.status = :completedStatus THEN b.bookingDate END), "
            + "SUM(CASE WHEN b.status = :completedStatus THEN 1L ELSE 0L END)) "
            + "FROM Customer c LEFT JOIN Booking b ON b.customer = c "
            + "GROUP BY c.customerId, c.fullName, c.mobileNo, c.adminNotes "
            + "ORDER BY c.fullName")
    List<CustomerStats> findCustomerStats(@Param("completedStatus") com.mirror.salon.booking.Booking.Status completedStatus);
}
