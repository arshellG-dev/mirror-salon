package com.mirror.salon.booking;

import com.mirror.salon.catalogue.SalonService;
import com.mirror.salon.catalogue.SalonServiceRepository;
import com.mirror.salon.users.Customer;
import com.mirror.salon.users.CustomerRepository;
import com.mirror.salon.users.Notification;
import com.mirror.salon.users.NotificationRepository;
import com.mirror.salon.users.OtpService;
import com.mirror.salon.users.OtpVerification;
import com.mirror.salon.users.Stylist;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingWorkflowService {

    private static final int SLOT_MINUTES = 30;
    private static final int RESCHEDULE_CANCEL_CUTOFF_HOURS = 24;

    private final BookingRepository bookingRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final StylistRepository stylistRepository;
    private final CustomerRepository customerRepository;
    private final AvailabilityService availabilityService;
    private final OtpService otpService;
    private final NotificationRepository notificationRepository;

    private void queueNotification(Booking booking, Notification.NotifType type, String message) {
        Notification notification = new Notification();
        notification.setCustomerId(booking.getCustomer().getCustomerId());
        notification.setBookingId(booking.getBookingId());
        notification.setNotifType(type);
        notification.setChannel(Notification.Channel.SMS);
        notification.setMessage(message);
        notification.setScheduledAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public static class BookingException extends RuntimeException {
        public BookingException(String message) { super(message); }
    }

    public int totalDurationRoundedTo30(List<SalonService> services) {
        int total = services.stream().mapToInt(SalonService::getDurationMins).sum();
        return ((total + SLOT_MINUTES - 1) / SLOT_MINUTES) * SLOT_MINUTES;
    }

    public record CreatedBooking(Booking booking, String demoOtp) {}

    public record StylistSummary(Integer staffId, String fullName) {}

    @Transactional(readOnly = true)
    public List<StylistSummary> stylistSummaries(java.util.Collection<Integer> stylistIds) {
        return stylistRepository.findAllById(stylistIds).stream()
                .map(s -> new StylistSummary(s.getStaffId(), s.getStaff().getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public String stylistName(Integer stylistId) {
        return stylistRepository.findById(stylistId)
                .map(s -> s.getStaff().getFullName())
                .orElse("Unknown");
    }

    @Transactional
    public String regenerateBookingOtp(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));
        OtpService.GeneratedOtp otp = otpService.generate(booking.getCustomer().getCustomerId(), bookingId,
                OtpVerification.Purpose.BOOKING_CONFIRMATION);
        return otp.plainCode();
    }

    @Transactional
    public CreatedBooking createPendingBooking(Integer customerId, List<Integer> serviceIds,
                                                Integer requestedStylistId, LocalDate date, LocalTime startTime) {
        if (serviceIds.isEmpty()) {
            throw new BookingException("Select at least one service.");
        }
        if (availabilityService.isSalonHoliday(date)) {
            throw new BookingException("The salon is closed on this date.");
        }

        List<SalonService> services = salonServiceRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new BookingException("One or more selected services could not be found.");
        }
        int totalDuration = totalDurationRoundedTo30(services);

        Integer stylistId = requestedStylistId;
        boolean autoAssigned = false;
        if (stylistId == null) {
            stylistId = availabilityService.autoAssign(serviceIds, date, startTime, totalDuration);
            autoAssigned = true;
            if (stylistId == null) {
                throw new BookingException("No stylist is available for the selected services at that time.");
            }
        } else {
            if (!availabilityService.qualifiedStylistIds(serviceIds).contains(stylistId)) {
                throw new BookingException("The chosen stylist cannot perform all selected services.");
            }
            List<LocalTime> free = availabilityService.freeSlots(stylistId, date, totalDuration);
            if (!free.contains(startTime)) {
                throw new BookingException("That time is no longer available for this stylist. Please pick another slot.");
            }
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BookingException("Customer not found."));
        Stylist stylist = stylistRepository.findById(stylistId)
                .orElseThrow(() -> new BookingException("Stylist not found."));

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setStylist(stylist);
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(totalDuration));
        booking.setStatus(Booking.Status.PENDING_OTP);
        booking.setSource(Booking.Source.ONLINE);
        booking.setAutoAssigned(autoAssigned);
        booking.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        for (SalonService service : services) {
            BookingService bs = new BookingService(booking, service, service.getPrice(), service.getDurationMins());
            booking.getServices().add(bs);
        }
        bookingRepository.save(booking);

        OtpService.GeneratedOtp otp = otpService.generate(customerId, booking.getBookingId(),
                OtpVerification.Purpose.BOOKING_CONFIRMATION);

        return new CreatedBooking(booking, otp.plainCode());
    }

    @Transactional
    public boolean confirmBooking(Integer bookingId, String code) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));
        if (booking.getStatus() != Booking.Status.PENDING_OTP) {
            throw new BookingException("This booking is not awaiting confirmation.");
        }
        OtpService.VerifyResult result = otpService.verifyForBooking(bookingId, code);
        if (result != OtpService.VerifyResult.SUCCESS) {
            return false;
        }
        booking.setStatus(Booking.Status.CONFIRMED);
        bookingRepository.save(booking);
        queueNotification(booking, Notification.NotifType.BOOKING_CONFIRMED,
                "Your booking at The Mirror is confirmed for " + booking.getBookingDate() + " at " + booking.getStartTime() + ".");
        return true;
    }

    @Transactional
    public void cancel(Integer bookingId, Integer customerId, Booking.CancelledBy cancelledBy, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));
        if (customerId != null && !booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new BookingException("You cannot manage this booking.");
        }
        requireOutsideCutoff(booking);
        booking.setStatus(Booking.Status.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(cancelledBy);
        booking.setCancelReason(reason);
        bookingRepository.save(booking);
        queueNotification(booking, Notification.NotifType.BOOKING_CANCELLED,
                "Your booking at The Mirror for " + booking.getBookingDate() + " has been cancelled.");
    }

    @Transactional
    public void reschedule(Integer bookingId, Integer customerId, LocalDate newDate, LocalTime newStartTime) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));
        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            throw new BookingException("You cannot manage this booking.");
        }
        requireOutsideCutoff(booking);

        int totalDuration = booking.getServices().stream()
                .mapToInt(BookingService::getDurationAtBooking).sum();

        if (availabilityService.isSalonHoliday(newDate)) {
            throw new BookingException("The salon is closed on that date.");
        }
        List<LocalTime> free = availabilityService.freeSlots(booking.getStylist().getStaffId(), newDate, totalDuration);
        if (!free.contains(newStartTime)) {
            throw new BookingException("That time is not available for your stylist. Please pick another slot.");
        }

        booking.setBookingDate(newDate);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newStartTime.plusMinutes(totalDuration));
        booking.setRescheduleCount(booking.getRescheduleCount() + 1);
        bookingRepository.save(booking);
        queueNotification(booking, Notification.NotifType.BOOKING_RESCHEDULED,
                "Your booking at The Mirror has been rescheduled to " + newDate + " at " + newStartTime + ".");
    }

    @Transactional
    public void markCompleted(Integer bookingId, Integer stylistId) {
        Booking booking = requireOwnedByStylist(bookingId, stylistId);
        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BookingException("Only confirmed bookings can be marked completed.");
        }
        booking.setStatus(Booking.Status.COMPLETED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void markNoShow(Integer bookingId, Integer stylistId) {
        Booking booking = requireOwnedByStylist(bookingId, stylistId);
        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BookingException("Only confirmed bookings can be marked as no-show.");
        }
        booking.setStatus(Booking.Status.NO_SHOW);
        bookingRepository.save(booking);
    }

    @Transactional
    public void stylistCancel(Integer bookingId, Integer stylistId, String reason) {
        Booking booking = requireOwnedByStylist(bookingId, stylistId);
        booking.setStatus(Booking.Status.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(Booking.CancelledBy.STYLIST);
        booking.setCancelReason(reason);
        bookingRepository.save(booking);
        queueNotification(booking, Notification.NotifType.BOOKING_CANCELLED,
                "Your booking at The Mirror for " + booking.getBookingDate() + " has been cancelled by the salon: " + reason);
    }

    private Booking requireOwnedByStylist(Integer bookingId, Integer stylistId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found."));
        if (!booking.getStylist().getStaffId().equals(stylistId)) {
            throw new BookingException("This booking does not belong to you.");
        }
        return booking;
    }

    private void requireOutsideCutoff(Booking booking) {
        LocalDateTime appointmentStart = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
        if (LocalDateTime.now().plusHours(RESCHEDULE_CANCEL_CUTOFF_HOURS).isAfter(appointmentStart)) {
            throw new BookingException("Bookings can only be changed at least 24 hours before the appointment.");
        }
    }
}
