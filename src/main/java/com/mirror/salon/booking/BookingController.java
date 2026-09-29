package com.mirror.salon.booking;

import com.mirror.salon.catalogue.SalonService;
import com.mirror.salon.catalogue.SalonServiceRepository;
import com.mirror.salon.users.Customer;
import com.mirror.salon.users.CustomerRepository;
import com.mirror.salon.users.CustomerSession;
import com.mirror.salon.users.OtpService;
import com.mirror.salon.users.OtpVerification;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Server-rendered booking wizard. Stepper tabs: 1 Service · 2 Date &amp; time
 * · 3 Login · 4 Confirm. Service/stylist selection and date/time picking are
 * open to anonymous visitors (matching the public "browse first" design);
 * login (OTP) is only required from step 3 onward, and the booking-
 * confirmation OTP is shown inline as part of step 4.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/booking")
public class BookingController {

    private static final String SK_SERVICE_IDS = "bookingServiceIds";
    private static final String SK_STYLIST_ID = "bookingStylistId";
    private static final String SK_AUTO = "bookingAuto";
    private static final String SK_DATE = "bookingDate";
    private static final String SK_START_TIME = "bookingStartTime";
    private static final String SK_PENDING_BOOKING_ID = "bookingPendingId";
    private static final String SK_LOGIN_PENDING_CUSTOMER_ID = "bookingLoginPendingCustomerId";

    private final SalonServiceRepository salonServiceRepository;
    private final AvailabilityService availabilityService;
    private final BookingWorkflowService bookingWorkflowService;
    private final CustomerRepository customerRepository;
    private final OtpService otpService;

    @SuppressWarnings("unchecked")
    private List<Integer> selectedServiceIds(HttpSession session) {
        Object stored = session.getAttribute(SK_SERVICE_IDS);
        return stored == null ? new ArrayList<>() : (List<Integer>) stored;
    }

    private void addStepper(Model model, int current) {
        model.addAttribute("step", current);
    }

    // ---------- Step 1: Service ----------

    @GetMapping("/new")
    public String startBooking(@RequestParam(required = false) Integer serviceId, HttpSession session, Model model) {
        List<Integer> preselected = new ArrayList<>();
        if (serviceId != null) {
            preselected.add(serviceId);
        }
        session.setAttribute(SK_SERVICE_IDS, preselected);

        model.addAttribute("services", salonServiceRepository.findByActiveTrueWithCategoryOrderByServiceId());
        model.addAttribute("selectedIds", preselected);
        addStepper(model, 1);
        return "booking/choose-services";
    }

    @PostMapping("/services")
    public String chooseServices(@RequestParam(required = false) List<Integer> serviceIds, HttpSession session, Model model) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            model.addAttribute("services", salonServiceRepository.findByActiveTrueWithCategoryOrderByServiceId());
            model.addAttribute("selectedIds", List.of());
            model.addAttribute("error", "Choose at least one service to continue.");
            addStepper(model, 1);
            return "booking/choose-services";
        }
        session.setAttribute(SK_SERVICE_IDS, serviceIds);
        return "redirect:/booking/stylist";
    }

    @GetMapping("/stylist")
    public String chooseStylistForm(HttpSession session, Model model) {
        List<Integer> serviceIds = selectedServiceIds(session);
        if (serviceIds.isEmpty()) {
            return "redirect:/booking/new";
        }
        Set<Integer> qualifiedIds = availabilityService.qualifiedStylistIds(serviceIds);
        model.addAttribute("qualifiedStylists", bookingWorkflowService.stylistSummaries(qualifiedIds));
        addStepper(model, 1);
        return "booking/choose-stylist";
    }

    @PostMapping("/stylist")
    public String chooseStylist(@RequestParam(required = false) Integer stylistId, HttpSession session) {
        boolean auto = stylistId == null || stylistId == 0;
        session.setAttribute(SK_STYLIST_ID, auto ? null : stylistId);
        session.setAttribute(SK_AUTO, auto);
        return "redirect:/booking/datetime";
    }

    // ---------- Step 2: Date & time ----------

    @GetMapping("/datetime")
    public String chooseDateTimeForm(@RequestParam(required = false) String date, HttpSession session, Model model) {
        List<Integer> serviceIds = selectedServiceIds(session);
        if (serviceIds.isEmpty()) {
            return "redirect:/booking/new";
        }
        List<SalonService> services = salonServiceRepository.findAllById(serviceIds);
        int totalDuration = bookingWorkflowService.totalDurationRoundedTo30(services);
        model.addAttribute("totalDuration", totalDuration);
        model.addAttribute("today", LocalDate.now());

        if (date != null && !date.isBlank()) {
            LocalDate parsedDate = LocalDate.parse(date);
            Integer stylistId = (Integer) session.getAttribute(SK_STYLIST_ID);
            boolean auto = Boolean.TRUE.equals(session.getAttribute(SK_AUTO));

            List<LocalTime> slots;
            if (auto) {
                Set<Integer> qualified = availabilityService.qualifiedStylistIds(serviceIds);
                Set<LocalTime> union = new java.util.TreeSet<>();
                for (Integer sid : qualified) {
                    union.addAll(availabilityService.freeSlots(sid, parsedDate, totalDuration));
                }
                slots = new ArrayList<>(union);
            } else {
                slots = availabilityService.freeSlots(stylistId, parsedDate, totalDuration);
            }
            model.addAttribute("selectedDate", date);
            model.addAttribute("slots", slots);
            model.addAttribute("isHoliday", availabilityService.isSalonHoliday(parsedDate));
        }
        addStepper(model, 2);
        return "booking/choose-datetime";
    }

    @PostMapping("/datetime")
    public String chooseDateTime(@RequestParam String date, @RequestParam String startTime, HttpSession session) {
        session.setAttribute(SK_DATE, date);
        session.setAttribute(SK_START_TIME, startTime);
        return "redirect:/booking/login";
    }

    // ---------- Step 3: Login (OTP) ----------

    @GetMapping("/login")
    public String loginStep(HttpSession session, Model model) {
        if (selectedServiceIds(session).isEmpty() || session.getAttribute(SK_DATE) == null) {
            return "redirect:/booking/new";
        }
        if (CustomerSession.isLoggedIn(session)) {
            return "redirect:/booking/confirm";
        }
        addStepper(model, 3);
        return "booking/login";
    }

    @PostMapping("/login")
    public String loginStepSubmit(@RequestParam String fullName, @RequestParam String mobileNo,
                                   HttpSession session, Model model) {
        String trimmedName = fullName.trim();
        String trimmedMobile = mobileNo.trim();
        if (!trimmedMobile.matches("^0[0-9]{9}$") || trimmedName.isBlank()) {
            model.addAttribute("error", "Enter your full name and a valid 10-digit mobile number.");
            addStepper(model, 3);
            return "booking/login";
        }

        Customer customer = customerRepository.findByMobileNo(trimmedMobile).orElseGet(() -> {
            Customer c = new Customer();
            c.setMobileNo(trimmedMobile);
            c.setRegisteredAt(LocalDateTime.now());
            return c;
        });
        customer.setFullName(trimmedName);
        customerRepository.save(customer);

        OtpService.GeneratedOtp generated = otpService.generate(customer.getCustomerId(), null, OtpVerification.Purpose.LOGIN);
        session.setAttribute(SK_LOGIN_PENDING_CUSTOMER_ID, customer.getCustomerId());

        model.addAttribute("demoOtp", generated.plainCode());
        model.addAttribute("mobileNo", trimmedMobile);
        model.addAttribute("showCodeField", true);
        addStepper(model, 3);
        return "booking/login";
    }

    @PostMapping("/login/verify")
    public String loginVerify(@RequestParam String code, HttpSession session, Model model) {
        Integer pendingCustomerId = (Integer) session.getAttribute(SK_LOGIN_PENDING_CUSTOMER_ID);
        if (pendingCustomerId == null) {
            return "redirect:/booking/login";
        }
        OtpService.VerifyResult result = otpService.verify(pendingCustomerId, OtpVerification.Purpose.LOGIN, code.trim());
        if (result == OtpService.VerifyResult.SUCCESS) {
            Customer customer = customerRepository.findById(pendingCustomerId).orElseThrow();
            customer.setVerified(true);
            customerRepository.save(customer);
            session.removeAttribute(SK_LOGIN_PENDING_CUSTOMER_ID);
            CustomerSession.login(session, customer);
            return "redirect:/booking/confirm";
        }
        model.addAttribute("error", "Incorrect code. Please try again.");
        model.addAttribute("showCodeField", true);
        model.addAttribute("mobileNo", customerRepository.findById(pendingCustomerId).map(Customer::getMobileNo).orElse(""));
        addStepper(model, 3);
        return "booking/login";
    }

    @PostMapping("/login/resend")
    public String loginResend(HttpSession session, Model model) {
        Integer pendingCustomerId = (Integer) session.getAttribute(SK_LOGIN_PENDING_CUSTOMER_ID);
        if (pendingCustomerId == null) {
            return "redirect:/booking/login";
        }
        OtpService.GeneratedOtp generated = otpService.generate(pendingCustomerId, null, OtpVerification.Purpose.LOGIN);
        model.addAttribute("demoOtp", generated.plainCode());
        model.addAttribute("showCodeField", true);
        model.addAttribute("mobileNo", customerRepository.findById(pendingCustomerId).map(Customer::getMobileNo).orElse(""));
        model.addAttribute("message", "A new code has been generated.");
        addStepper(model, 3);
        return "booking/login";
    }

    // ---------- Step 4: Confirm (booking OTP) ----------

    @GetMapping("/confirm")
    public String confirmForm(HttpSession session, Model model) {
        if (!CustomerSession.isLoggedIn(session)) {
            return "redirect:/booking/login";
        }
        List<Integer> serviceIds = selectedServiceIds(session);
        if (serviceIds.isEmpty() || session.getAttribute(SK_DATE) == null) {
            return "redirect:/booking/new";
        }
        List<SalonService> services = salonServiceRepository.findAllById(serviceIds);
        Integer stylistId = (Integer) session.getAttribute(SK_STYLIST_ID);
        boolean auto = Boolean.TRUE.equals(session.getAttribute(SK_AUTO));

        model.addAttribute("services", services);
        model.addAttribute("totalDuration", bookingWorkflowService.totalDurationRoundedTo30(services));
        model.addAttribute("totalPrice", services.stream()
                .map(SalonService::getPrice).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        model.addAttribute("auto", auto);
        model.addAttribute("stylistName", auto ? "Any available stylist" : bookingWorkflowService.stylistName(stylistId));
        model.addAttribute("date", session.getAttribute(SK_DATE));
        model.addAttribute("startTime", session.getAttribute(SK_START_TIME));
        addStepper(model, 4);
        return "booking/confirm";
    }

    @PostMapping("/confirm")
    public String confirm(HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/booking/login";
        }
        List<Integer> serviceIds = selectedServiceIds(session);
        Integer stylistId = (Integer) session.getAttribute(SK_STYLIST_ID);
        LocalDate date = LocalDate.parse((String) session.getAttribute(SK_DATE));
        LocalTime startTime = LocalTime.parse((String) session.getAttribute(SK_START_TIME));

        try {
            BookingWorkflowService.CreatedBooking created = bookingWorkflowService.createPendingBooking(
                    customerId, serviceIds, stylistId, date, startTime);
            session.setAttribute(SK_PENDING_BOOKING_ID, created.booking().getBookingId());
            model.addAttribute("demoOtp", created.demoOtp());
            addStepper(model, 4);
            return "booking/otp";
        } catch (BookingWorkflowService.BookingException e) {
            model.addAttribute("error", e.getMessage());
            return confirmForm(session, model);
        }
    }

    @PostMapping("/otp")
    public String verifyOtp(@RequestParam String code, HttpSession session, Model model) {
        Integer bookingId = (Integer) session.getAttribute(SK_PENDING_BOOKING_ID);
        if (bookingId == null) {
            return "redirect:/booking/new";
        }
        boolean ok = bookingWorkflowService.confirmBooking(bookingId, code.trim());
        if (ok) {
            session.removeAttribute(SK_SERVICE_IDS);
            session.removeAttribute(SK_STYLIST_ID);
            session.removeAttribute(SK_AUTO);
            session.removeAttribute(SK_DATE);
            session.removeAttribute(SK_START_TIME);
            session.removeAttribute(SK_PENDING_BOOKING_ID);
            return "redirect:/bookings/my";
        }
        model.addAttribute("error", "Incorrect or expired code. Please try again.");
        addStepper(model, 4);
        return "booking/otp";
    }

    @PostMapping("/otp/resend")
    public String resendOtp(HttpSession session, Model model) {
        Integer bookingId = (Integer) session.getAttribute(SK_PENDING_BOOKING_ID);
        if (bookingId == null) {
            return "redirect:/booking/new";
        }
        String code = bookingWorkflowService.regenerateBookingOtp(bookingId);
        model.addAttribute("demoOtp", code);
        model.addAttribute("message", "A new code has been generated.");
        addStepper(model, 4);
        return "booking/otp";
    }
}
