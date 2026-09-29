package com.mirror.salon.orders;

import com.mirror.salon.users.CustomerSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MyOrdersController {

    private final ProductOrderRepository productOrderRepository;

    @GetMapping("/orders/my")
    public String myOrders(HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", productOrderRepository.findByCustomerIdWithDetails(customerId));
        return "orders/my-orders";
    }
}
