package com.mirror.salon.booking;

import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import com.mirror.salon.users.Stylist;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/unavailability")
@PreAuthorize("hasRole('STYLIST')")
public class StylistAvailabilityController {

    private static final String[] DAY_NAMES = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private final StaffRepository staffRepository;
    private final StylistRepository stylistRepository;
    private final StylistUnavailabilityRepository unavailabilityRepository;
    private final StylistScheduleService stylistScheduleService;

    private Integer currentStylistId(Authentication authentication) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        return staff.getStaffId();
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        Integer stylistId = currentStylistId(authentication);
        model.addAttribute("blocks", unavailabilityRepository.findByStylist_StaffId(stylistId));

        java.util.Map<Integer, StylistWorkingHours> hoursByDay = new java.util.LinkedHashMap<>();
        stylistScheduleService.workingHoursFor(stylistId).forEach(h -> hoursByDay.put(h.getDayOfWeek(), h));
        model.addAttribute("hoursByDay", hoursByDay);
        model.addAttribute("dayNames", DAY_NAMES);
        return "staff/unavailability";
    }

    @PostMapping
    public String add(@RequestParam String blockDate, @RequestParam String startTime, @RequestParam String endTime,
                       @RequestParam(required = false) String reason,
                       Authentication authentication, RedirectAttributes redirectAttributes) {
        Integer stylistId = currentStylistId(authentication);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        if (!end.isAfter(start)) {
            redirectAttributes.addFlashAttribute("error", "End time must be after start time.");
            return "redirect:/staff/unavailability";
        }
        Stylist stylist = stylistRepository.findById(stylistId).orElseThrow();
        StylistUnavailability block = new StylistUnavailability();
        block.setStylist(stylist);
        block.setBlockDate(LocalDate.parse(blockDate));
        block.setStartTime(start);
        block.setEndTime(end);
        block.setReason(reason);
        unavailabilityRepository.save(block);
        redirectAttributes.addFlashAttribute("message", "Unavailable time added.");
        return "redirect:/staff/unavailability";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String blockDate, @RequestParam String startTime,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        Integer stylistId = currentStylistId(authentication);
        StylistUnavailabilityId id = new StylistUnavailabilityId(stylistId, LocalDate.parse(blockDate), LocalTime.parse(startTime));
        unavailabilityRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Removed.");
        return "redirect:/staff/unavailability";
    }
}
