package com.mirror.salon.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SalonHolidayRepository extends JpaRepository<SalonHoliday, LocalDate> {

    List<SalonHoliday> findByHolidayDateGreaterThanEqualOrderByHolidayDate(LocalDate from);
}
