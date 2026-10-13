package com.mirror.salon;

import com.mirror.salon.catalogue.SalonServiceRepository;
import com.mirror.salon.catalogue.ServiceCatalogService;
import com.mirror.salon.orders.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ServiceCatalogService serviceCatalogService;
    private final SalonServiceRepository salonServiceRepository;
    private final ReviewService reviewService;

    @GetMapping("/")
    public String home(Model model) {
        List<ServiceCatalogService.CategorySummary> summaries = serviceCatalogService.categorySummaries();
        model.addAttribute("faceBody", summaries.subList(0, 4));
        model.addAttribute("hair", summaries.subList(4, 9));

        List<String> signatureNames = List.of("Layer Cut", "Hair Ironing", "Keratin Treatment");
        model.addAttribute("signature", salonServiceRepository.findAllWithCategory().stream()
                .filter(s -> signatureNames.contains(s.getServiceName()))
                .sorted((a, b) -> Integer.compare(signatureNames.indexOf(a.getServiceName()), signatureNames.indexOf(b.getServiceName())))
                .toList());

        salonServiceRepository.findAllWithCategory().stream()
                .filter(s -> s.getServiceName().equals("Hydra Facial")).findFirst()
                .ifPresent(s -> model.addAttribute("heroService", s));

        model.addAttribute("reviews", reviewService.visibleReviews());
        model.addAttribute("averageRating", reviewService.averageRating());
        return "home/index";
    }
}
