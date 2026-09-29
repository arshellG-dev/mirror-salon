package com.mirror.salon.booking;

import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/my-bookings")
@PreAuthorize("hasRole('STYLIST')")
public class StylistBookingsController {

    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;
    private final BookingWorkflowService bookingWorkflowService;

    private Integer currentStylistId(Authentication authentication) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        return staff.getStaffId();
    }

    /** Agenda/week view (spec §3.2 "My Schedule"), default to the current week. */
    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week,
                        Authentication authentication, Model model) {
        Integer stylistId = currentStylistId(authentication);
        LocalDate anchor = week != null ? week : LocalDate.now();
        LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Booking> bookings = bookingRepository.findByStylistIdAndDateBetweenWithDetails(stylistId, weekStart, weekEnd);
        Map<LocalDate, List<Booking>> byDay = new LinkedHashMap<>();
        for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
            LocalDate day = d;
            byDay.put(day, bookings.stream().filter(b -> b.getBookingDate().equals(day)).toList());
        }

        model.addAttribute("byDay", byDay);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("prevWeek", weekStart.minusWeeks(1));
        model.addAttribute("nextWeek", weekStart.plusWeeks(1));
        model.addAttribute("now", LocalDateTime.now());
        return "staff/my-bookings";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            bookingWorkflowService.markCompleted(id, currentStylistId(authentication));
            redirectAttributes.addFlashAttribute("message", "Marked as completed.");
        } catch (BookingWorkflowService.BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/my-bookings";
    }

    @PostMapping("/{id}/no-show")
    public String noShow(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            bookingWorkflowService.markNoShow(id, currentStylistId(authentication));
            redirectAttributes.addFlashAttribute("message", "Marked as no-show.");
        } catch (BookingWorkflowService.BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/my-bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Integer id, @RequestParam String reason,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            bookingWorkflowService.stylistCancel(id, currentStylistId(authentication), reason);
            redirectAttributes.addFlashAttribute("message", "Booking cancelled.");
        } catch (BookingWorkflowService.BookingException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/my-bookings";
    }
}
