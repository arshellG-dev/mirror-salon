package com.mirror.salon.booking;

import com.mirror.salon.users.Stylist;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * Manages a stylist's recurring weekly working hours (admin-set, per the
 * spec's decision that base working hours are admin-managed, not
 * self-service). Read-only for the stylist themselves — see
 * StylistAvailabilityController for their own one-off unavailability blocks.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StylistScheduleService {

    private final StylistWorkingHoursRepository workingHoursRepository;
    private final StylistRepository stylistRepository;

    @Transactional(readOnly = true)
    public List<StylistWorkingHours> workingHoursFor(Integer stylistId) {
        return workingHoursRepository.findByStylist_StaffId(stylistId);
    }

    /** A null start or end clears that day (treated as a day off). */
    public void setDay(Integer stylistId, int dayOfWeek, LocalTime start, LocalTime end) {
        StylistWorkingHoursId id = new StylistWorkingHoursId(stylistId, dayOfWeek);
        if (start == null || end == null) {
            workingHoursRepository.deleteById(id);
            return;
        }
        StylistWorkingHours hours = workingHoursRepository.findByStylist_StaffIdAndDayOfWeek(stylistId, dayOfWeek)
                .orElseGet(() -> {
                    StylistWorkingHours h = new StylistWorkingHours();
                    Stylist stylist = stylistRepository.findById(stylistId).orElseThrow();
                    h.setStylist(stylist);
                    h.setDayOfWeek(dayOfWeek);
                    return h;
                });
        hours.setStartTime(start);
        hours.setEndTime(end);
        workingHoursRepository.save(hours);
    }
}
