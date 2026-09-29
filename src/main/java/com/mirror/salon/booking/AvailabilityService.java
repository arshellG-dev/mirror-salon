package com.mirror.salon.booking;

import com.mirror.salon.catalogue.StylistServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final int SLOT_STEP_MINUTES = 30;

    private final SalonHolidayRepository salonHolidayRepository;
    private final StylistWorkingHoursRepository workingHoursRepository;
    private final StylistUnavailabilityRepository unavailabilityRepository;
    private final BookingRepository bookingRepository;
    private final StylistServiceRepository stylistServiceRepository;

    public boolean isSalonHoliday(LocalDate date) {
        return salonHolidayRepository.existsById(date);
    }

    /** Qualified stylist ids able to perform every one of the given services. */
    public Set<Integer> qualifiedStylistIds(Collection<Integer> serviceIds) {
        Set<Integer> result = null;
        for (Integer serviceId : serviceIds) {
            Set<Integer> stylistsForThisService = new HashSet<>();
            stylistServiceRepository.findByService_ServiceId(serviceId)
                    .forEach(ss -> stylistsForThisService.add(ss.getStylist().getStaffId()));
            if (result == null) {
                result = stylistsForThisService;
            } else {
                result.retainAll(stylistsForThisService);
            }
        }
        return result == null ? Set.of() : result;
    }

    /** Free 30-minute start times for the given stylist/date that can fit a booking of totalDurationMinutes. */
    public List<LocalTime> freeSlots(Integer stylistId, LocalDate date, int totalDurationMinutes) {
        if (isSalonHoliday(date)) {
            return List.of();
        }

        int dayOfWeek = date.getDayOfWeek().getValue(); // 1=Monday..7=Sunday, matches DB convention
        var workingHours = workingHoursRepository.findByStylist_StaffIdAndDayOfWeek(stylistId, dayOfWeek);
        if (workingHours.isEmpty()) {
            return List.of();
        }

        LocalTime workStart = workingHours.get().getStartTime();
        LocalTime workEnd = workingHours.get().getEndTime();

        List<StylistUnavailability> blocks = unavailabilityRepository.findByStylist_StaffIdAndBlockDate(stylistId, date);
        List<Booking> activeBookings = bookingRepository.findByStylist_StaffIdAndBookingDate(stylistId, date).stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED || b.getStatus() == Booking.Status.PENDING_OTP)
                .toList();

        List<LocalTime> free = new ArrayList<>();
        int startMinute = workStart.toSecondOfDay() / 60;
        int endMinute = workEnd.toSecondOfDay() / 60;

        for (int minute = startMinute; minute + totalDurationMinutes <= endMinute; minute += SLOT_STEP_MINUTES) {
            LocalTime candidateStart = LocalTime.of(minute / 60, minute % 60);
            LocalTime candidateEnd = candidateStart.plusMinutes(totalDurationMinutes);

            boolean blocked = blocks.stream().anyMatch(b ->
                    candidateStart.isBefore(b.getEndTime()) && candidateEnd.isAfter(b.getStartTime()));

            boolean overlapsBooking = activeBookings.stream().anyMatch(b ->
                    candidateStart.isBefore(b.getEndTime()) && candidateEnd.isAfter(b.getStartTime()));

            if (!blocked && !overlapsBooking) {
                free.add(candidateStart);
            }
        }
        return free;
    }

    /** Among qualified stylists free for the whole slot, pick the one with fewest bookings that day. */
    public Integer autoAssign(Collection<Integer> serviceIds, LocalDate date, LocalTime startTime, int totalDurationMinutes) {
        Set<Integer> qualified = qualifiedStylistIds(serviceIds);

        Integer best = null;
        long bestCount = Long.MAX_VALUE;
        for (Integer stylistId : qualified) {
            List<LocalTime> slots = freeSlots(stylistId, date, totalDurationMinutes);
            if (!slots.contains(startTime)) {
                continue;
            }
            long count = bookingRepository.findByStylist_StaffIdAndBookingDate(stylistId, date).stream()
                    .filter(b -> b.getStatus() == Booking.Status.CONFIRMED || b.getStatus() == Booking.Status.PENDING_OTP)
                    .count();
            if (count < bestCount) {
                bestCount = count;
                best = stylistId;
            }
        }
        return best;
    }
}
