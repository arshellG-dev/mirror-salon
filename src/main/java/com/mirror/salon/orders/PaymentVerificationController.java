package com.mirror.salon.orders;

import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/payments")
@PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
public class PaymentVerificationController {

    private final StaffRepository staffRepository;
    private final PaymentVerificationService paymentVerificationService;
    private final BankTransferPaymentRepository bankTransferPaymentRepository;

    private Integer currentStaffId(Authentication authentication) {
        Staff staff = staffRepository.findByUsername(authentication.getName()).orElseThrow();
        return staff.getStaffId();
    }

    @GetMapping
    public String list(Model model) {
        var pending = paymentVerificationService.pendingPayments();
        Map<Integer, BankTransferPayment> slips = new HashMap<>();
        for (Payment p : pending) {
            if (p.getPaymentMethod() == Payment.PaymentMethod.BANK_TRANSFER) {
                bankTransferPaymentRepository.findById(p.getPaymentId()).ifPresent(bt -> slips.put(p.getPaymentId(), bt));
            }
        }
        model.addAttribute("payments", pending);
        model.addAttribute("slips", slips);
        return "staff/payments";
    }

    @PostMapping("/{id}/verify")
    public String verify(@PathVariable Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            paymentVerificationService.verify(id, currentStaffId(authentication));
            redirectAttributes.addFlashAttribute("message", "Payment verified and order confirmed.");
        } catch (PaymentVerificationService.VerificationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/payments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Integer id, @RequestParam String reason,
                          Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            paymentVerificationService.reject(id, currentStaffId(authentication), reason);
            redirectAttributes.addFlashAttribute("message", "Payment rejected.");
        } catch (PaymentVerificationService.VerificationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/payments";
    }
}
