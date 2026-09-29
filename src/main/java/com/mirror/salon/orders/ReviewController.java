package com.mirror.salon.orders;

import com.mirror.salon.users.CustomerSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/reviews")
    public String list(Model model) {
        model.addAttribute("reviews", reviewService.visibleReviews());
        model.addAttribute("averageRating", reviewService.averageRating());
        return "reviews/list";
    }

    @PostMapping("/reviews")
    public String submit(@RequestParam int rating, @RequestParam(required = false) String comment,
                          HttpSession session, RedirectAttributes redirectAttributes) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            reviewService.submit(customerId, rating, comment);
            redirectAttributes.addFlashAttribute("message", "Thank you for your review!");
        } catch (ReviewService.ReviewException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reviews";
    }
}
