package com.mirror.salon.orders;

import com.mirror.salon.catalogue.DeliveryAreaRepository;
import com.mirror.salon.catalogue.Product;
import com.mirror.salon.catalogue.ProductRepository;
import com.mirror.salon.users.CustomerSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final ProductRepository productRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;
    private final OrderCheckoutService orderCheckoutService;

    @GetMapping("/checkout")
    public String checkoutForm(HttpSession session, Model model) {
        if (!CustomerSession.isLoggedIn(session)) {
            return "redirect:/login";
        }
        if (Cart.items(session).isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("areas", deliveryAreaRepository.findByActiveTrue());
        model.addAttribute("fullName", CustomerSession.getFullName(session));
        return "orders/checkout";
    }

    @PostMapping("/checkout/cod")
    public String checkoutCod(@RequestParam Integer areaId, @RequestParam String recipientName,
                               @RequestParam String contactNo, @RequestParam String addressLine,
                               @RequestParam String city, HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            OrderCheckoutService.CheckoutForm form = new OrderCheckoutService.CheckoutForm(
                    customerId, Cart.items(session), areaId, recipientName, contactNo, addressLine, city);
            ProductOrder order = orderCheckoutService.placeCodOrder(form);
            Cart.clear(session);
            return "redirect:/orders/" + order.getOrderId() + "/thank-you";
        } catch (OrderCheckoutService.CheckoutException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("areas", deliveryAreaRepository.findByActiveTrue());
            return "orders/checkout";
        }
    }

    @PostMapping("/checkout/bank-transfer")
    public String checkoutBankTransfer(@RequestParam Integer areaId, @RequestParam String recipientName,
                                        @RequestParam String contactNo, @RequestParam String addressLine,
                                        @RequestParam String city, @RequestParam(required = false) String bankReference,
                                        @RequestParam("slip") MultipartFile slip,
                                        HttpSession session, Model model) {
        Integer customerId = CustomerSession.getCustomerId(session);
        if (customerId == null) {
            return "redirect:/login";
        }
        try {
            OrderCheckoutService.CheckoutForm form = new OrderCheckoutService.CheckoutForm(
                    customerId, Cart.items(session), areaId, recipientName, contactNo, addressLine, city);
            ProductOrder order = orderCheckoutService.placeBankTransferOrder(form, slip, bankReference);
            Cart.clear(session);
            return "redirect:/orders/" + order.getOrderId() + "/thank-you";
        } catch (OrderCheckoutService.CheckoutException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("areas", deliveryAreaRepository.findByActiveTrue());
            return "orders/checkout";
        }
    }

    @GetMapping("/orders/{id}/thank-you")
    public String thankYou(@org.springframework.web.bind.annotation.PathVariable Integer id, Model model) {
        model.addAttribute("orderId", id);
        return "orders/thank-you";
    }
}
