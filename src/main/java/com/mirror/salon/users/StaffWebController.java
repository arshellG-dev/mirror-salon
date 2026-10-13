package com.mirror.salon.users;

import com.mirror.salon.booking.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequiredArgsConstructor
public class StaffWebController {

    private final StaffRepository staffRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/staff/login")
    public String loginForm(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String logout,
                             Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out.");
        }
        return "staff/login";
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        if (staff.getStaffType() == Staff.StaffType.ADMIN) {
            return "redirect:/admin";
        }
        if (staff.getStaffType() == Staff.StaffType.STYLIST) {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);
            var todaysBookings = bookingRepository.findByStylistIdAndDateWithDetails(staff.getStaffId(), today);
            model.addAttribute("staff", staff);
            model.addAttribute("todaysBookings", todaysBookings);
            model.addAttribute("todayCount", todaysBookings.size());
            model.addAttribute("weekCount", bookingRepository.countByStylist_StaffIdAndBookingDateBetween(staff.getStaffId(), weekStart, weekEnd));
            return "staff/dashboard";
        }
        model.addAttribute("staff", staff);
        return "staff/dashboard-legacy";
    }
}
