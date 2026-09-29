package com.mirror.salon.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StylistWorkingHoursRepository extends JpaRepository<StylistWorkingHours, StylistWorkingHoursId> {

    Optional<StylistWorkingHours> findByStylist_StaffIdAndDayOfWeek(Integer stylistId, Integer dayOfWeek);

    List<StylistWorkingHours> findByStylist_StaffId(Integer stylistId);
}
