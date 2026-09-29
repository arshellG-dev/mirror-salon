package com.mirror.salon.catalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping("/services")
    public String list(Model model) {
        model.addAttribute("summaries", serviceCatalogService.categorySummaries());
        return "services/list";
    }

    @GetMapping("/services/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        ServiceCategory category = serviceCatalogService.getCategory(id);
        model.addAttribute("category", category);
        model.addAttribute("sections", serviceCatalogService.getPriceSections(id));
        model.addAttribute("group", id <= 4 ? "Face & Body" : "Hair");
        model.addAttribute("prev", serviceCatalogService.adjacentCategory(id, false));
        model.addAttribute("next", serviceCatalogService.adjacentCategory(id, true));
        return "services/detail";
    }
}
