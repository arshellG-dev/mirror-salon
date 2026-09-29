package com.mirror.salon.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StylistUnavailabilityRepository extends JpaRepository<StylistUnavailability, StylistUnavailabilityId> {

    List<StylistUnavailability> findByStylist_StaffIdAndBlockDate(Integer stylistId, LocalDate blockDate);

    List<StylistUnavailability> findByStylist_StaffId(Integer stylistId);
}
