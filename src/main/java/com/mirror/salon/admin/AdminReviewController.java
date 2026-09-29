package com.mirror.salon.admin;

import com.mirror.salon.orders.ReviewService;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;
    private final StaffRepository staffRepository;

    private Integer currentStaffId(Authentication authentication) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        return staff.getStaffId();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("reviews", reviewService.allForModeration());
        return "admin/reviews";
    }

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        reviewService.setHidden(id, true, currentStaffId(authentication));
        redirectAttributes.addFlashAttribute("message", "Review hidden.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/unhide")
    public String unhide(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        reviewService.setHidden(id, false, currentStaffId(authentication));
        redirectAttributes.addFlashAttribute("message", "Review unhidden.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        reviewService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Review deleted.");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Integer id, @RequestParam String replyText,
                         Authentication authentication, RedirectAttributes redirectAttributes) {
        reviewService.reply(id, replyText, currentStaffId(authentication));
        redirectAttributes.addFlashAttribute("message", "Reply posted.");
        return "redirect:/admin/reviews";
    }
}
