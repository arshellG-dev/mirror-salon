package com.mirror.salon.orders;

import com.mirror.salon.catalogue.DeliveryAreaRepository;
import com.mirror.salon.catalogue.Product;
import com.mirror.salon.catalogue.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductRepository productRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;

    public record CartLine(Product product, int quantity, BigDecimal lineTotal) {}

    @PostMapping("/cart/add")
    public String add(@RequestParam Integer productId, @RequestParam(defaultValue = "1") int quantity, HttpSession session) {
        Cart.add(session, productId, Math.max(1, quantity));
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Integer productId, @RequestParam int quantity, HttpSession session) {
        Cart.setQuantity(session, productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Integer productId, HttpSession session) {
        Cart.remove(session, productId);
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String view(HttpSession session, Model model) {
        Map<Integer, Integer> items = Cart.items(session);
        List<CartLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            Product product = productRepository.findById(entry.getKey()).orElse(null);
            if (product == null) continue;
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
            lines.add(new CartLine(product, entry.getValue(), lineTotal));
            subtotal = subtotal.add(lineTotal);
        }
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("areas", deliveryAreaRepository.findByActiveTrue());
        return "orders/cart";
    }
}
