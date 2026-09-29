package com.mirror.salon.users;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class CustomerAuthController {

    private static final String PENDING_CUSTOMER_ID = "pendingLoginCustomerId";
    private static final String PENDING_MOBILE_NO = "pendingLoginMobileNo";

    private final CustomerRepository customerRepository;
    private final OtpService otpService;

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        if (CustomerSession.isLoggedIn(session)) {
            return "redirect:/";
        }
        return "customer/login";
    }

    @PostMapping("/login")
    public String requestOtp(@RequestParam String fullName,
                              @RequestParam String mobileNo,
                              HttpSession session,
                              Model model) {
        String trimmedFullName = fullName.trim();
        String trimmedMobileNo = mobileNo.trim();

        if (!trimmedMobileNo.matches("^0[0-9]{9}$")) {
            model.addAttribute("error", "Enter a valid 10-digit mobile number starting with 0.");
            model.addAttribute("fullName", trimmedFullName);
            model.addAttribute("mobileNo", trimmedMobileNo);
            return "customer/login";
        }
        if (trimmedFullName.isBlank()) {
            model.addAttribute("error", "Please enter your full name.");
            model.addAttribute("mobileNo", trimmedMobileNo);
            return "customer/login";
        }

        Customer customer = customerRepository.findByMobileNo(trimmedMobileNo).orElseGet(() -> {
            Customer c = new Customer();
            c.setMobileNo(trimmedMobileNo);
            c.setRegisteredAt(LocalDateTime.now());
            return c;
        });
        customer.setFullName(trimmedFullName);
        customerRepository.save(customer);

        OtpService.GeneratedOtp generated =
                otpService.generate(customer.getCustomerId(), null, OtpVerification.Purpose.LOGIN);

        session.setAttribute(PENDING_CUSTOMER_ID, customer.getCustomerId());
        session.setAttribute(PENDING_MOBILE_NO, customer.getMobileNo());

        model.addAttribute("demoOtp", generated.plainCode());
        model.addAttribute("mobileNo", customer.getMobileNo());
        return "customer/verify-login";
    }

    @PostMapping("/login/verify")
    public String verifyOtp(@RequestParam String code,
                             HttpSession session,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Integer pendingCustomerId = (Integer) session.getAttribute(PENDING_CUSTOMER_ID);
        if (pendingCustomerId == null) {
            return "redirect:/login";
        }

        OtpService.VerifyResult result =
                otpService.verify(pendingCustomerId, OtpVerification.Purpose.LOGIN, code.trim());

        if (result == OtpService.VerifyResult.SUCCESS) {
            Customer customer = customerRepository.findById(pendingCustomerId).orElseThrow();
            customer.setVerified(true);
            customerRepository.save(customer);

            session.removeAttribute(PENDING_CUSTOMER_ID);
            session.removeAttribute(PENDING_MOBILE_NO);
            CustomerSession.login(session, customer);

            redirectAttributes.addFlashAttribute("message", "Welcome, " + customer.getFullName() + "!");
            return "redirect:/";
        }

        String error = switch (result) {
            case TOO_MANY_ATTEMPTS -> "Too many wrong attempts. Please request a new code.";
            case EXPIRED_OR_LOCKED -> "This code has expired. Please request a new code.";
            default -> "Incorrect code. Please try again.";
        };
        model.addAttribute("error", error);
        model.addAttribute("mobileNo", session.getAttribute(PENDING_MOBILE_NO));
        return "customer/verify-login";
    }

    @PostMapping("/login/resend")
    public String resendOtp(HttpSession session, Model model) {
        Integer pendingCustomerId = (Integer) session.getAttribute(PENDING_CUSTOMER_ID);
        if (pendingCustomerId == null) {
            return "redirect:/login";
        }
        OtpService.GeneratedOtp generated =
                otpService.generate(pendingCustomerId, null, OtpVerification.Purpose.LOGIN);
        model.addAttribute("demoOtp", generated.plainCode());
        model.addAttribute("mobileNo", session.getAttribute(PENDING_MOBILE_NO));
        model.addAttribute("message", "A new code has been generated.");
        return "customer/verify-login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        CustomerSession.logout(session);
        return "redirect:/";
    }
}
