package com.mirror.salon.catalogue;

import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/my-services")
@PreAuthorize("hasRole('STYLIST')")
public class StylistServicesController {

    private final StaffRepository staffRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final StylistServiceManagementService managementService;

    private Integer currentStylistId(Authentication authentication) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        return staff.getStaffId();
    }

    @GetMapping
    public String view(Authentication authentication, Model model) {
        Integer stylistId = currentStylistId(authentication);
        List<SalonService> allServices = salonServiceRepository.findByActiveTrue();
        Set<Integer> myServiceIds = managementService.myServiceIds(stylistId);

        model.addAttribute("services", allServices);
        model.addAttribute("myServiceIds", myServiceIds);
        return "staff/my-services";
    }

    @PostMapping
    public String update(@RequestParam(required = false) List<Integer> serviceIds,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        Set<Integer> desired = serviceIds == null ? new HashSet<>() : new HashSet<>(serviceIds);
        managementService.setMyUnrestrictedServices(currentStylistId(authentication), desired);
        redirectAttributes.addFlashAttribute("message", "Your services have been updated.");
        return "redirect:/staff/my-services";
    }
}
