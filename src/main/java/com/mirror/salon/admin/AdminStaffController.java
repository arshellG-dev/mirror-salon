package com.mirror.salon.admin;

import com.mirror.salon.booking.StylistScheduleService;
import com.mirror.salon.booking.StylistWorkingHours;
import com.mirror.salon.catalogue.SalonServiceRepository;
import com.mirror.salon.catalogue.StylistServiceRepository;
import com.mirror.salon.orders.FileStorageService;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffManagementService;
import com.mirror.salon.users.StaffRepository;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {

    private static final String[] DAY_NAMES = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private final StaffManagementService staffManagementService;
    private final StaffRepository staffRepository;
    private final StylistRepository stylistRepository;
    private final StylistServiceRepository stylistServiceRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final StylistScheduleService stylistScheduleService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("staffList", staffManagementService.listAll());
        return "admin/staff-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("staffTypes", Staff.StaffType.values());
        return "admin/staff-form";
    }

    @PostMapping
    public String create(@RequestParam String firstName, @RequestParam String lastName,
                          @RequestParam(required = false) String mobileNo, @RequestParam String username,
                          @RequestParam String password, @RequestParam Staff.StaffType staffType,
                          @RequestParam(required = false) String bio, @RequestParam(required = false) String specialtyTitle,
                          RedirectAttributes redirectAttributes, Model model) {
        try {
            staffManagementService.create(firstName, lastName, mobileNo, username, password, staffType, bio, specialtyTitle);
            redirectAttributes.addFlashAttribute("message", "Staff member added.");
            return "redirect:/admin/staff";
        } catch (StaffManagementService.StaffException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("staffTypes", Staff.StaffType.values());
            return "admin/staff-form";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        staffManagementService.setActive(id, false);
        redirectAttributes.addFlashAttribute("message", "Staff member deactivated.");
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        staffManagementService.setActive(id, true);
        redirectAttributes.addFlashAttribute("message", "Staff member reactivated.");
        return "redirect:/admin/staff";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Integer id, @RequestParam String password, RedirectAttributes redirectAttributes) {
        staffManagementService.resetPassword(id, password);
        redirectAttributes.addFlashAttribute("message", "Password reset.");
        return "redirect:/admin/staff";
    }

    // ---- Stylist profile (bio/photo/specialty, assigned services, working hours) ----

    @GetMapping("/{id}")
    public String profile(@PathVariable Integer id, Model model) {
        model.addAttribute("stylist", stylistRepository.findByIdWithStaff(id).orElseThrow());
        model.addAttribute("services", salonServiceRepository.findAllWithCategory());
        model.addAttribute("assignedServiceIds", stylistServiceRepository.findByStylist_StaffId(id).stream()
                .map(ss -> ss.getService().getServiceId()).toList());

        Map<Integer, StylistWorkingHours> hoursByDay = new LinkedHashMap<>();
        stylistScheduleService.workingHoursFor(id).forEach(h -> hoursByDay.put(h.getDayOfWeek(), h));
        model.addAttribute("hoursByDay", hoursByDay);
        model.addAttribute("dayNames", DAY_NAMES);
        return "admin/staff-profile";
    }

    @PostMapping("/{id}/profile")
    public String updateProfile(@PathVariable Integer id, @RequestParam(required = false) String bio,
                                 @RequestParam(required = false) String specialtyTitle,
                                 @RequestParam(required = false) MultipartFile photo,
                                 RedirectAttributes redirectAttributes) {
        String photoUrl = (photo != null && !photo.isEmpty()) ? fileStorageService.storePhoto(photo) : null;
        staffManagementService.updateStylistProfile(id, bio, specialtyTitle, photoUrl);
        redirectAttributes.addFlashAttribute("message", "Profile updated.");
        return "redirect:/admin/staff/" + id;
    }

    @PostMapping("/{id}/working-hours")
    public String updateWorkingHours(@PathVariable Integer id, @RequestParam Map<String, String> form,
                                      RedirectAttributes redirectAttributes) {
        for (int day = 1; day <= 7; day++) {
            String start = form.get("start" + day);
            String end = form.get("end" + day);
            LocalTime startTime = (start == null || start.isBlank()) ? null : LocalTime.parse(start);
            LocalTime endTime = (end == null || end.isBlank()) ? null : LocalTime.parse(end);
            stylistScheduleService.setDay(id, day, startTime, endTime);
        }
        redirectAttributes.addFlashAttribute("message", "Working hours updated.");
        return "redirect:/admin/staff/" + id;
    }
}
