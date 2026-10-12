package com.mirror.salon.admin;

import com.mirror.salon.catalogue.DeliveryArea;
import com.mirror.salon.catalogue.DeliveryAreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/delivery-areas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryAreaController {

    private final DeliveryAreaRepository deliveryAreaRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("areas", deliveryAreaRepository.findAll());
        return "admin/delivery-areas";
    }

    @PostMapping
    public String add(@RequestParam String areaName, @RequestParam BigDecimal deliveryFee,
                       RedirectAttributes redirectAttributes) {
        DeliveryArea area = new DeliveryArea();
        area.setAreaName(areaName);
        area.setDeliveryFee(deliveryFee);
        area.setActive(true);
        deliveryAreaRepository.save(area);
        redirectAttributes.addFlashAttribute("message", "Delivery area added.");
        return "redirect:/admin/delivery-areas";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @RequestParam String areaName, @RequestParam BigDecimal deliveryFee,
                          @RequestParam(defaultValue = "false") boolean active, RedirectAttributes redirectAttributes) {
        DeliveryArea area = deliveryAreaRepository.findById(id).orElseThrow();
        area.setAreaName(areaName);
        area.setDeliveryFee(deliveryFee);
        area.setActive(active);
        deliveryAreaRepository.save(area);
        redirectAttributes.addFlashAttribute("message", "Delivery area updated.");
        return "redirect:/admin/delivery-areas";
    }
}
