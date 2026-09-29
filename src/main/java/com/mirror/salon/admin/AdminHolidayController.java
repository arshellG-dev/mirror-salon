package com.mirror.salon.admin;

import com.mirror.salon.booking.SalonHoliday;
import com.mirror.salon.booking.SalonHolidayRepository;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/holidays")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHolidayController {

    private final SalonHolidayRepository salonHolidayRepository;
    private final StaffRepository staffRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("holidays", salonHolidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDate(LocalDate.now()));
        return "admin/holidays";
    }

    @PostMapping
    public String add(@RequestParam String holidayDate, @RequestParam String description,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        SalonHoliday holiday = new SalonHoliday();
        holiday.setHolidayDate(LocalDate.parse(holidayDate));
        holiday.setDescription(description);
        holiday.setDeclaredBy(staff);
        salonHolidayRepository.save(holiday);
        redirectAttributes.addFlashAttribute("message", "Holiday added.");
        return "redirect:/admin/holidays";
    }

    @PostMapping("/{date}/delete")
    public String delete(@PathVariable("date") String date, RedirectAttributes redirectAttributes) {
        salonHolidayRepository.deleteById(LocalDate.parse(date));
        redirectAttributes.addFlashAttribute("message", "Holiday removed.");
        return "redirect:/admin/holidays";
    }
}
