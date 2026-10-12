package com.mirror.salon.catalogue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Covers ServiceCatalogService.getPriceSections(), the Full/Half pairing
 * logic that drives every service-detail price card (e.g. waxing services
 * that come in "Full" and "Half" variants must render as one row with two
 * prices, not two separate rows).
 */
@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private SalonServiceRepository salonServiceRepository;
    @Mock
    private ServiceCategoryRepository serviceCategoryRepository;
    @Mock
    private StylistServiceRepository stylistServiceRepository;

    private ServiceCatalogService service;

    @BeforeEach
    void setUp() {
        service = new ServiceCatalogService(salonServiceRepository, serviceCategoryRepository, stylistServiceRepository);
    }

    private SalonService makeService(String description, String name, String price) {
        SalonService s = new SalonService();
        ServiceCategory category = new ServiceCategory();
        category.setCategoryName("Test Category");
        s.setCategory(category);
        s.setDescription(description);
        s.setServiceName(name);
        s.setPrice(new BigDecimal(price));
        s.setDurationMins(30);
        return s;
    }

    @Test
    void pairsFullAndHalfServicesWithTheSameDescriptionIntoOneRow() {
        SalonService full = makeService("Waxing", "Underarm", "500.00");
        SalonService half = makeService("Waxing", "Underarm (Half)", "300.00");
        SalonService unrelated = makeService("Massage", "Full Body", "1000.00");

        when(salonServiceRepository.findByActiveTrueOrderByServiceId())
                .thenReturn(List.of(full, half, unrelated));

        List<ServiceCatalogService.PriceSection> sections = service.getPriceSections(null);

        assertThat(sections).hasSize(2);

        ServiceCatalogService.PriceSection waxingSection = sections.get(0);
        assertThat(waxingSection.title()).isEqualTo("Waxing");
        assertThat(waxingSection.hasHalf()).isTrue();
        assertThat(waxingSection.rows()).hasSize(1);
        assertThat(waxingSection.rows().get(0).name()).isEqualTo("Underarm");
        assertThat(waxingSection.rows().get(0).full()).isEqualTo(full);
        assertThat(waxingSection.rows().get(0).half()).isEqualTo(half);

        ServiceCatalogService.PriceSection massageSection = sections.get(1);
        assertThat(massageSection.title()).isEqualTo("Massage");
        assertThat(massageSection.hasHalf()).isFalse();
        assertThat(massageSection.rows()).hasSize(1);
        assertThat(massageSection.rows().get(0).full()).isEqualTo(unrelated);
        assertThat(massageSection.rows().get(0).half()).isNull();
    }

    @Test
    void aHalfServiceWithNoMatchingFullServiceStandsAloneInItsOwnRow() {
        SalonService halfOnly = makeService("Waxing", "Legs (Half)", "400.00");

        when(salonServiceRepository.findByActiveTrueOrderByServiceId())
                .thenReturn(List.of(halfOnly));

        List<ServiceCatalogService.PriceSection> sections = service.getPriceSections(null);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).rows()).hasSize(1);
        assertThat(sections.get(0).rows().get(0).full()).isNull();
        assertThat(sections.get(0).rows().get(0).half()).isEqualTo(halfOnly);
    }
}
