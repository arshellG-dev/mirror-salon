package com.mirror.salon.booking;

import com.mirror.salon.users.CustomerSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MyBookingsController {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;
    private final BookingWorkflowService bookingWorkflowService;

    @GetMapping("/bookings/my")
    public String myBookings(HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        model.addAttribute("bookings", bookingRepository.findByCustomerIdWithDetails(customerId));
        return "booking/my-bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            bookingWorkflowService.cancel(id, customerId, Booking.CancelledBy.CUSTOMER, "Cancelled by customer");
            redirectAttributes.addFlashAttribute("message", "Booking cancelled.");
        } catch (BookingWorkflowService.BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bookings/my";
    }

    @GetMapping("/bookings/{id}/reschedule")
    public String rescheduleForm(@PathVariable Integer id, @RequestParam(required = false) String date,
                                  HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        Booking booking = bookingRepository.findByIdWithDetails(id).orElseThrow();
        if (!booking.getCustomer().getCustomerId().equals(customerId)) {
            return "redirect:/bookings/my";
        }
        model.addAttribute("booking", booking);
        model.addAttribute("today", LocalDate.now());

        if (date != null && !date.isBlank()) {
            LocalDate parsedDate = LocalDate.parse(date);
            int totalDuration = booking.getServices().stream()
                    .mapToInt(BookingService::getDurationAtBooking).sum();
            List<LocalTime> slots = availabilityService.freeSlots(booking.getStylist().getStaffId(), parsedDate, totalDuration);
            model.addAttribute("selectedDate", date);
            model.addAttribute("slots", slots);
            model.addAttribute("isHoliday", availabilityService.isSalonHoliday(parsedDate));
        }
        return "booking/reschedule";
    }

    @PostMapping("/bookings/{id}/reschedule")
    public String reschedule(@PathVariable Integer id, @RequestParam String date, @RequestParam String startTime,
                              HttpSession session, RedirectAttributes redirectAttributes) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            bookingWorkflowService.reschedule(id, customerId, LocalDate.parse(date), LocalTime.parse(startTime));
            redirectAttributes.addFlashAttribute("message", "Booking rescheduled.");
            return "redirect:/bookings/my";
        } catch (BookingWorkflowService.BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/bookings/" + id + "/reschedule";
        }
    }
}
