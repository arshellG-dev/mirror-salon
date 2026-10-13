package com.mirror.salon.admin;

import com.mirror.salon.booking.BookingRepository;
import com.mirror.salon.catalogue.SalonServiceRepository;
import com.mirror.salon.users.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private static final int MAX_PAST_VISIT_ROWS = 6;

    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final StylistRepository stylistRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final StaffRepository staffRepository;
    private final CustomerBackfillService customerBackfillService;

    @GetMapping
    public String list(Model model) {
        var stats = customerRepository.findCustomerStats(com.mirror.salon.booking.Booking.Status.COMPLETED);
        Map<Integer, Long> monthsAsCustomer = new HashMap<>();
        for (var s : stats) {
            if (s.firstVisitDate() != null) {
                monthsAsCustomer.put(s.customerId(), ChronoUnit.MONTHS.between(s.firstVisitDate(), LocalDate.now()));
            }
        }
        model.addAttribute("stats", stats);
        model.addAttribute("monthsAsCustomer", monthsAsCustomer);
        return "admin/customers";
    }

    @PostMapping
    public String addWalkIn(@RequestParam String fullName, @RequestParam String mobileNo,
                             @RequestParam(required = false) String adminNotes, RedirectAttributes redirectAttributes) {
        if (customerRepository.findByMobileNo(mobileNo).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "A customer with that mobile number already exists.");
            return "redirect:/admin/customers";
        }
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setMobileNo(mobileNo);
        customer.setVerified(true);
        customer.setCreatedVia(Customer.CreatedVia.ADMIN);
        customer.setAdminNotes(adminNotes);
        customer.setRegisteredAt(LocalDateTime.now());
        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("message", "Customer added.");
        return "redirect:/admin/customers";
    }

    @GetMapping("/{id}")
    public String profile(@PathVariable Integer id, Model model) {
        model.addAttribute("customer", customerRepository.findById(id).orElseThrow());
        model.addAttribute("visits", bookingRepository.findByCustomerIdWithDetails(id));
        return "admin/customer-profile";
    }

    @PostMapping("/{id}/notes")
    public String saveNotes(@PathVariable Integer id, @RequestParam(required = false) String adminNotes,
                             RedirectAttributes redirectAttributes) {
        Customer customer = customerRepository.findById(id).orElseThrow();
        customer.setAdminNotes(adminNotes);
        customerRepository.save(customer);
        redirectAttributes.addFlashAttribute("message", "Notes saved.");
        return "redirect:/admin/customers/" + id;
    }

    @GetMapping("/past/new")
    public String newPastCustomerForm(Model model) {
        model.addAttribute("stylists", stylistRepository.findAllWithStaff());
        model.addAttribute("services", salonServiceRepository.findAllWithCategory());
        model.addAttribute("rows", java.util.stream.IntStream.rangeClosed(1, MAX_PAST_VISIT_ROWS).boxed().toList());
        return "admin/customer-past-form";
    }

    @PostMapping("/past")
    public String createPastCustomer(@RequestParam String fullName, @RequestParam String mobileNo,
                                      @RequestParam(required = false) String adminNotes,
                                      HttpServletRequest request, Authentication authentication,
                                      RedirectAttributes redirectAttributes, Model model) {
        List<CustomerBackfillService.PastVisit> visits = new ArrayList<>();
        for (int i = 1; i <= MAX_PAST_VISIT_ROWS; i++) {
            String date = request.getParameter("visitDate" + i);
            String stylistId = request.getParameter("visitStylist" + i);
            String[] serviceIds = request.getParameterValues("visitServices" + i);
            if (date == null || date.isBlank() || stylistId == null || stylistId.isBlank() || serviceIds == null || serviceIds.length == 0) {
                continue;
            }
            visits.add(new CustomerBackfillService.PastVisit(
                    LocalDate.parse(date), Integer.valueOf(stylistId),
                    Arrays.stream(serviceIds).map(Integer::valueOf).toList()));
        }
        Integer staffId = staffRepository.findByUsername(authentication.getName()).orElseThrow().getStaffId();
        try {
            Customer customer = customerBackfillService.createWithHistory(fullName, mobileNo, adminNotes, visits, staffId);
            redirectAttributes.addFlashAttribute("message", "Customer added with " + visits.size() + " past visit(s).");
            return "redirect:/admin/customers/" + customer.getCustomerId();
        } catch (CustomerBackfillService.BackfillException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("stylists", stylistRepository.findAllWithStaff());
            model.addAttribute("services", salonServiceRepository.findAllWithCategory());
            model.addAttribute("rows", java.util.stream.IntStream.rangeClosed(1, MAX_PAST_VISIT_ROWS).boxed().toList());
            return "admin/customer-past-form";
        }
    }
}
