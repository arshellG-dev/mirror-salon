package com.mirror.salon.admin;

import com.mirror.salon.booking.Booking;
import com.mirror.salon.booking.BookingRepository;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin-wide visibility into every booking (spec §2.2) — read-only: this is
 * a reporting page, not where bookings are managed day-to-day (that stays
 * the stylist's job via /staff/my-bookings).
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final StylistRepository stylistRepository;

    @GetMapping("/admin/bookings")
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) Integer stylistId,
                        @RequestParam(required = false) Booking.Status status,
                        Model model) {
        List<Booking> bookings = bookingRepository.findAllWithDetails().stream()
                .filter(b -> from == null || !b.getBookingDate().isBefore(from))
                .filter(b -> to == null || !b.getBookingDate().isAfter(to))
                .filter(b -> stylistId == null || b.getStylist().getStaffId().equals(stylistId))
                .filter(b -> status == null || b.getStatus() == status)
                .toList();

        model.addAttribute("bookings", bookings);
        model.addAttribute("stylists", stylistRepository.findAllWithStaff());
        model.addAttribute("statuses", Booking.Status.values());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("stylistId", stylistId);
        model.addAttribute("status", status);
        return "admin/bookings-list";
    }

    @GetMapping("/admin/bookings/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("booking", bookingRepository.findByIdWithDetails(id).orElseThrow());
        return "admin/booking-detail";
    }
}
