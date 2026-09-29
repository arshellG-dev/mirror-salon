package com.mirror.salon.catalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductCatalogService productCatalogService;
    private final ProductCategoryRepository productCategoryRepository;

    @GetMapping("/products")
    public String list(@RequestParam(required = false) Integer categoryId, Model model) {
        model.addAttribute("products", productCatalogService.browse(categoryId));
        model.addAttribute("categories", productCategoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        return "products/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productCatalogService.getDetail(id));
        return "products/detail";
    }
}
