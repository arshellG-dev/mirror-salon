package com.mirror.salon.booking;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingServiceRepository extends JpaRepository<BookingService, BookingServiceId> {
}
