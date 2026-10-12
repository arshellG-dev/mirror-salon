package com.mirror.salon.admin;

import com.mirror.salon.catalogue.CatalogueAdminService;
import com.mirror.salon.catalogue.ProductCategoryRepository;
import com.mirror.salon.catalogue.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final CatalogueAdminService catalogueAdminService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("serviceCategories", serviceCategoryRepository.findAllByOrderByCategoryId());
        model.addAttribute("productCategories", productCategoryRepository.findAllByOrderByCategoryId());
        return "admin/categories";
    }

    @PostMapping("/services")
    public String addServiceCategory(@RequestParam String categoryName, @RequestParam(required = false) String description,
                                      RedirectAttributes redirectAttributes) {
        catalogueAdminService.createServiceCategory(categoryName, description);
        redirectAttributes.addFlashAttribute("message", "Service category added.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/services/{id}")
    public String updateServiceCategory(@PathVariable Integer id, @RequestParam String categoryName,
                                         @RequestParam(required = false) String description,
                                         RedirectAttributes redirectAttributes) {
        catalogueAdminService.updateServiceCategory(id, categoryName, description);
        redirectAttributes.addFlashAttribute("message", "Service category updated.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/services/{id}/delete")
    public String deleteServiceCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            catalogueAdminService.deleteServiceCategory(id);
            redirectAttributes.addFlashAttribute("message", "Service category removed.");
        } catch (CatalogueAdminService.CatalogueException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/products")
    public String addProductCategory(@RequestParam String categoryName, @RequestParam(required = false) String description,
                                      RedirectAttributes redirectAttributes) {
        catalogueAdminService.createProductCategory(categoryName, description);
        redirectAttributes.addFlashAttribute("message", "Product category added.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/products/{id}")
    public String updateProductCategory(@PathVariable Integer id, @RequestParam String categoryName,
                                         @RequestParam(required = false) String description,
                                         RedirectAttributes redirectAttributes) {
        catalogueAdminService.updateProductCategory(id, categoryName, description);
        redirectAttributes.addFlashAttribute("message", "Product category updated.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProductCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            catalogueAdminService.deleteProductCategory(id);
            redirectAttributes.addFlashAttribute("message", "Product category removed.");
        } catch (CatalogueAdminService.CatalogueException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
