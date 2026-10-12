package com.mirror.salon.admin;

import com.mirror.salon.catalogue.*;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogueController {

    private final SalonServiceRepository salonServiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CatalogueAdminService catalogueAdminService;
    private final StylistRepository stylistRepository;
    private final StylistServiceRepository stylistServiceRepository;
    private final StylistServiceManagementService stylistServiceManagementService;

    // ---- Services ----

    @GetMapping("/services")
    public String services(Model model) {
        model.addAttribute("services", salonServiceRepository.findAllWithCategory());
        return "admin/service-list";
    }

    @GetMapping("/services/new")
    public String newServiceForm(Model model) {
        model.addAttribute("categories", serviceCategoryRepository.findAll());
        return "admin/service-form";
    }

    @PostMapping("/services")
    public String createService(@RequestParam String serviceName, @RequestParam String description,
                                 @RequestParam BigDecimal price, @RequestParam int durationMins,
                                 @RequestParam Integer categoryId, @RequestParam(defaultValue = "false") boolean restricted,
                                 RedirectAttributes redirectAttributes) {
        catalogueAdminService.createService(serviceName, description, price, durationMins, categoryId, restricted);
        redirectAttributes.addFlashAttribute("message", "Service added.");
        return "redirect:/admin/services";
    }

    @GetMapping("/services/{id}/edit")
    public String editServiceForm(@PathVariable Integer id, Model model) {
        model.addAttribute("service", salonServiceRepository.findById(id).orElseThrow());
        model.addAttribute("categories", serviceCategoryRepository.findAll());
        Set<Integer> assignedStylistIds = new HashSet<>();
        stylistServiceRepository.findByService_ServiceId(id).forEach(ss -> assignedStylistIds.add(ss.getStylist().getStaffId()));
        model.addAttribute("assignedStylistIds", assignedStylistIds);
        model.addAttribute("stylists", stylistRepository.findAllWithStaff());
        return "admin/service-edit";
    }

    @PostMapping("/services/{id}")
    public String updateService(@PathVariable Integer id, @RequestParam String serviceName, @RequestParam String description,
                                 @RequestParam BigDecimal price, @RequestParam int durationMins,
                                 @RequestParam Integer categoryId, @RequestParam(defaultValue = "false") boolean restricted,
                                 @RequestParam(defaultValue = "false") boolean active,
                                 RedirectAttributes redirectAttributes) {
        catalogueAdminService.updateService(id, serviceName, description, price, durationMins, categoryId, restricted, active);
        redirectAttributes.addFlashAttribute("message", "Service updated.");
        return "redirect:/admin/services";
    }

    @PostMapping("/services/{serviceId}/stylists/{stylistId}")
    public String assignStylist(@PathVariable Integer serviceId, @PathVariable Integer stylistId,
                                 @RequestParam boolean assign, RedirectAttributes redirectAttributes) {
        stylistServiceManagementService.adminSetStylistService(stylistId, serviceId, assign);
        redirectAttributes.addFlashAttribute("message", "Stylist assignment updated.");
        return "redirect:/admin/services/" + serviceId + "/edit";
    }

    // ---- Products ----

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("products", productRepository.findAllWithCategory());
        return "admin/product-list";
    }

    @GetMapping("/products/new")
    public String newProductForm(Model model) {
        model.addAttribute("categories", productCategoryRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(@RequestParam String productName, @RequestParam(required = false) String brand,
                                 @RequestParam(required = false) String size, @RequestParam(required = false) String description,
                                 @RequestParam BigDecimal price, @RequestParam int stockQty, @RequestParam Integer categoryId,
                                 RedirectAttributes redirectAttributes) {
        catalogueAdminService.createProduct(productName, brand, size, description, price, stockQty, categoryId);
        redirectAttributes.addFlashAttribute("message", "Product added.");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/{id}/edit")
    public String editProductForm(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productRepository.findById(id).orElseThrow());
        model.addAttribute("categories", productCategoryRepository.findAll());
        return "admin/product-edit";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Integer id, @RequestParam String productName,
                                 @RequestParam(required = false) String brand, @RequestParam(required = false) String size,
                                 @RequestParam(required = false) String description, @RequestParam BigDecimal price,
                                 @RequestParam int stockQty, @RequestParam Integer categoryId,
                                 @RequestParam(defaultValue = "false") boolean active,
                                 RedirectAttributes redirectAttributes) {
        catalogueAdminService.updateProduct(id, productName, brand, size, description, price, stockQty, categoryId, active);
        redirectAttributes.addFlashAttribute("message", "Product updated.");
        return "redirect:/admin/products";
    }
}
