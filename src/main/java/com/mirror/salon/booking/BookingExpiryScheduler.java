package com.mirror.salon.booking;

import com.mirror.salon.users.Notification;
import com.mirror.salon.users.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Scheduled housekeeping jobs for bookings: OTP expiry sweep and day-before reminders. */
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private static final int PENDING_OTP_TIMEOUT_MINUTES = 10;

    private final BookingRepository bookingRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_OTP_TIMEOUT_MINUTES);
        List<Booking> stale = bookingRepository.findByStatusAndCreatedAtBefore(Booking.Status.PENDING_OTP, cutoff);
        for (Booking booking : stale) {
            booking.setStatus(Booking.Status.EXPIRED);
        }
        bookingRepository.saveAll(stale);
    }

    /** Queues a reminder for each confirmed booking happening tomorrow that hasn't had one queued yet. */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void queueTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Booking> upcoming = bookingRepository.findByStatusAndBookingDate(Booking.Status.CONFIRMED, tomorrow);
        for (Booking booking : upcoming) {
            if (notificationRepository.existsByBookingIdAndNotifType(booking.getBookingId(), Notification.NotifType.BOOKING_REMINDER)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setCustomerId(booking.getCustomer().getCustomerId());
            notification.setBookingId(booking.getBookingId());
            notification.setNotifType(Notification.NotifType.BOOKING_REMINDER);
            notification.setChannel(Notification.Channel.SMS);
            notification.setMessage("Reminder: your appointment at The Mirror is tomorrow at " + booking.getStartTime() + ".");
            notification.setScheduledAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
}
