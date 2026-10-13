package com.mirror.salon.users;

import com.mirror.salon.booking.Booking;
import com.mirror.salon.booking.BookingRepository;
import com.mirror.salon.booking.BookingService;
import com.mirror.salon.catalogue.SalonService;
import com.mirror.salon.catalogue.SalonServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Backfills a pre-website regular's real visit history in one go (spec
 * §2.9 "Add Past Customer"). Each past visit becomes a real, completed,
 * HISTORICAL-sourced Booking so it shows up in reports and the customer's
 * visit history exactly like any other completed appointment — priced at
 * today's service price since we have no record of what it cost at the time.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerBackfillService {

    private static final LocalTime NOMINAL_START = LocalTime.of(10, 0);

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final StylistRepository stylistRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final StaffRepository staffRepository;

    public static class BackfillException extends RuntimeException {
        public BackfillException(String message) { super(message); }
    }

    public record PastVisit(LocalDate date, Integer stylistId, List<Integer> serviceIds) {}

    public Customer createWithHistory(String fullName, String mobileNo, String adminNotes,
                                       List<PastVisit> visits, Integer enteredByStaffId) {
        if (customerRepository.findByMobileNo(mobileNo).isPresent()) {
            throw new BackfillException("A customer with that mobile number already exists.");
        }
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setMobileNo(mobileNo);
        customer.setVerified(true);
        customer.setCreatedVia(Customer.CreatedVia.ADMIN);
        customer.setAdminNotes(adminNotes);
        customer.setRegisteredAt(LocalDateTime.now());
        customerRepository.save(customer);

        for (PastVisit visit : visits) {
            if (visit.date() == null || visit.stylistId() == null || visit.serviceIds() == null || visit.serviceIds().isEmpty()) {
                continue;
            }
            Stylist stylist = stylistRepository.findById(visit.stylistId())
                    .orElseThrow(() -> new BackfillException("Stylist not found."));

            Booking booking = new Booking();
            booking.setCustomer(customer);
            booking.setStylist(stylist);
            booking.setCreatedByStaff(staffRepository.getReferenceById(enteredByStaffId));
            booking.setBookingDate(visit.date());
            booking.setStatus(Booking.Status.COMPLETED);
            booking.setSource(Booking.Source.HISTORICAL);
            booking.setAutoAssigned(false);
            booking.setRescheduleCount(0);
            booking.setCreatedAt(LocalDateTime.now());

            LocalTime cursor = NOMINAL_START;
            for (Integer serviceId : visit.serviceIds()) {
                SalonService service = salonServiceRepository.findById(serviceId)
                        .orElseThrow(() -> new BackfillException("Service not found."));
                BookingService line = new BookingService();
                line.setBooking(booking);
                line.setService(service);
                line.setPriceAtBooking(service.getPrice());
                line.setDurationAtBooking(service.getDurationMins());
                booking.getServices().add(line);
                cursor = cursor.plusMinutes(service.getDurationMins());
            }
            booking.setStartTime(NOMINAL_START);
            booking.setEndTime(cursor);
            bookingRepository.save(booking);
        }
        return customer;
    }
}
