package com.mirror.salon.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRevenueController {

    private final RevenueRepository revenueRepository;

    @GetMapping("/admin/revenue")
    public String revenue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           Model model) {
        var serviceRevenue = revenueRepository.serviceRevenue(from, to);
        var productRevenue = revenueRepository.productRevenue(from, to);
        var deliveryRevenue = revenueRepository.deliveryRevenueByArea(from, to);

        BigDecimal totalServiceRevenue = revenueRepository.serviceRevenueTotal(from, to);
        BigDecimal totalProductRevenue = revenueRepository.productRevenueTotal(from, to);
        BigDecimal totalDeliveryRevenue = deliveryRevenue.stream()
                .map(DeliveryRevenueRow::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxServiceRow = serviceRevenue.stream().map(ServiceRevenueRow::getRevenue)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        BigDecimal maxProductRow = productRevenue.stream().map(ProductRevenueRow::getRevenue)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        BigDecimal maxDeliveryRow = deliveryRevenue.stream().map(DeliveryRevenueRow::getRevenue)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ONE);

        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("serviceRevenue", serviceRevenue);
        model.addAttribute("productRevenue", productRevenue);
        model.addAttribute("deliveryRevenue", deliveryRevenue);
        model.addAttribute("totalServiceRevenue", totalServiceRevenue);
        model.addAttribute("totalProductRevenue", totalProductRevenue);
        model.addAttribute("totalDeliveryRevenue", totalDeliveryRevenue);
        model.addAttribute("maxServiceRow", maxServiceRow.max(BigDecimal.ONE));
        model.addAttribute("maxProductRow", maxProductRow.max(BigDecimal.ONE));
        model.addAttribute("maxDeliveryRow", maxDeliveryRow.max(BigDecimal.ONE));
        return "admin/revenue";
    }
}
