package com.mirror.salon.admin;

import com.mirror.salon.booking.BookingRepository;
import com.mirror.salon.catalogue.ProductRepository;
import com.mirror.salon.orders.Payment;
import com.mirror.salon.orders.PaymentRepository;
import com.mirror.salon.orders.ReviewRepository;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final BookingRepository bookingRepository;
    private final RevenueRepository revenueRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final StaffRepository staffRepository;

    @GetMapping("/admin")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal serviceRevenue = revenueRepository.serviceRevenueTotal(monthStart, monthEnd);
        BigDecimal productRevenue = revenueRepository.productRevenueTotal(monthStart, monthEnd);

        model.addAttribute("todaysBookingCount", bookingRepository.countByBookingDate(today));
        model.addAttribute("monthRevenue", serviceRevenue.add(productRevenue));
        model.addAttribute("pendingPaymentCount", paymentRepository.countByPaymentStatus(Payment.PaymentStatus.PENDING));
        model.addAttribute("unrepliedReviewCount", reviewRepository.countByReplyTextIsNull());
        model.addAttribute("lowStockCount", productRepository.countByActiveTrueAndStockQtyLessThanEqual(LOW_STOCK_THRESHOLD));
        model.addAttribute("activeStaffCount", staffRepository.countByActiveTrue());
        model.addAttribute("todaysSchedule", bookingRepository.findByBookingDateWithDetails(today));
        return "admin/dashboard";
    }
}
