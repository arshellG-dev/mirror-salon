package com.mirror.salon.users;

import com.mirror.salon.orders.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Spec §3.5 "My Profile" — a stylist editing their own bio/photo/specialty. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/my-profile")
@PreAuthorize("hasRole('STYLIST')")
public class StylistProfileController {

    private final StaffRepository staffRepository;
    private final StylistRepository stylistRepository;
    private final StaffManagementService staffManagementService;
    private final FileStorageService fileStorageService;

    private Integer currentStylistId(Authentication authentication) {
        return staffRepository.findByUsername(authentication.getName()).orElseThrow().getStaffId();
    }

    @GetMapping
    public String form(Authentication authentication, Model model) {
        model.addAttribute("stylist", stylistRepository.findByIdWithStaff(currentStylistId(authentication)).orElseThrow());
        return "staff/my-profile";
    }

    @PostMapping
    public String save(Authentication authentication, @RequestParam(required = false) String bio,
                        @RequestParam(required = false) String specialtyTitle,
                        @RequestParam(required = false) MultipartFile photo,
                        RedirectAttributes redirectAttributes) {
        Integer stylistId = currentStylistId(authentication);
        String photoUrl = (photo != null && !photo.isEmpty()) ? fileStorageService.storePhoto(photo) : null;
        staffManagementService.updateStylistProfile(stylistId, bio, specialtyTitle, photoUrl);
        redirectAttributes.addFlashAttribute("message", "Profile updated.");
        return "redirect:/staff/my-profile";
    }
}
