package com.mirror.salon.catalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final SalonServiceRepository salonServiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final StylistServiceRepository stylistServiceRepository;

    /** A row in a service-detail price card: a plain service, or a Full/Half pair (e.g. waxing). */
    public record PriceRowView(String name, SalonService full, SalonService half) {}

    /** One price card on the detail page (e.g. "Sothys", "Full & half"). */
    public record PriceSection(String title, boolean hasHalf, List<PriceRowView> rows) {}

    /** A row on the services list / home price panels. */
    public record CategorySummary(ServiceCategory category, long serviceCount, BigDecimal fromPrice, List<String> serviceNames) {}

    public List<SalonService> browse(Integer categoryId) {
        List<SalonService> services = categoryId == null
                ? salonServiceRepository.findByActiveTrueOrderByServiceId()
                : salonServiceRepository.findByActiveTrueAndCategory_CategoryIdOrderByServiceId(categoryId);
        services.forEach(s -> s.getCategory().getCategoryName());
        return services;
    }

    public SalonService getDetail(Integer serviceId) {
        SalonService service = salonServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));
        service.getCategory().getCategoryName();
        service.getProductsUsed().size();
        return service;
    }

    public ServiceCategory getCategory(Integer categoryId) {
        return serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    public List<ServiceCategory> allCategories() {
        return serviceCategoryRepository.findAllByOrderByCategoryId();
    }

    /** category id of the previous/next category, wrapping around (used for the detail-page pager). */
    public ServiceCategory adjacentCategory(Integer categoryId, boolean next) {
        List<ServiceCategory> all = allCategories();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getCategoryId().equals(categoryId)) { idx = i; break; }
        }
        int size = all.size();
        int targetIdx = next ? (idx + 1) % size : (idx - 1 + size) % size;
        return all.get(targetIdx);
    }

    public List<CategorySummary> categorySummaries() {
        List<CategorySummary> summaries = new ArrayList<>();
        for (ServiceCategory category : allCategories()) {
            List<SalonService> services = salonServiceRepository
                    .findByActiveTrueAndCategory_CategoryIdOrderByServiceId(category.getCategoryId());
            long conceptualCount = services.stream().filter(s -> !isHalf(s)).count();
            BigDecimal from = services.stream().map(SalonService::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            List<String> names = services.stream().filter(s -> !isHalf(s)).map(SalonService::getServiceName).toList();
            summaries.add(new CategorySummary(category, conceptualCount, from, names));
        }
        return summaries;
    }

    /** Groups a category's services into price-card sections, pairing "X" with "X (Half)" rows. */
    public List<PriceSection> getPriceSections(Integer categoryId) {
        List<SalonService> services = browse(categoryId);
        List<PriceSection> sections = new ArrayList<>();
        String currentTitle = null;
        List<PriceRowView> currentRows = null;
        SalonService pendingFull = null;

        for (SalonService s : services) {
            String title = s.getDescription() == null ? "" : s.getDescription();
            if (!title.equals(currentTitle)) {
                if (pendingFull != null) { currentRows.add(new PriceRowView(pendingFull.getServiceName(), pendingFull, null)); pendingFull = null; }
                if (currentRows != null) sections.add(new PriceSection(currentTitle, hasHalfRow(currentRows), currentRows));
                currentTitle = title;
                currentRows = new ArrayList<>();
            }
            if (isHalf(s)) {
                String baseName = s.getServiceName().replace(" (Half)", "");
                if (pendingFull != null && pendingFull.getServiceName().equals(baseName)) {
                    currentRows.add(new PriceRowView(baseName, pendingFull, s));
                    pendingFull = null;
                } else {
                    currentRows.add(new PriceRowView(baseName, null, s));
                }
            } else {
                if (pendingFull != null) { currentRows.add(new PriceRowView(pendingFull.getServiceName(), pendingFull, null)); }
                pendingFull = s;
            }
        }
        if (pendingFull != null) { currentRows.add(new PriceRowView(pendingFull.getServiceName(), pendingFull, null)); }
        if (currentRows != null) sections.add(new PriceSection(currentTitle, hasHalfRow(currentRows), currentRows));
        return sections;
    }

    private boolean isHalf(SalonService s) {
        return s.getServiceName().endsWith(" (Half)");
    }

    private boolean hasHalfRow(List<PriceRowView> rows) {
        return rows.stream().anyMatch(r -> r.half() != null);
    }

    public List<StylistService> getQualifiedStylists(Integer serviceId) {
        List<StylistService> results = stylistServiceRepository.findByService_ServiceId(serviceId);
        results.forEach(ss -> ss.getStylist().getStaff().getFirstName());
        return results;
    }
}
