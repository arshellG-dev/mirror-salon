package com.mirror.salon.config;

import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the logged-in staff member's name and role available to every
 * template (used by the internal dashboard's top bar) without every
 * admin/staff controller having to add it to its own Model.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentStaffAdvice {

    private final StaffRepository staffRepository;

    @ModelAttribute
    public void addCurrentStaff(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        staffRepository.findByUsername(authentication.getName()).ifPresent(staff -> {
            model.addAttribute("currentStaffName", staff.getFullName());
            model.addAttribute("currentStaffRole", staff.getStaffType().name());
        });
    }
}
