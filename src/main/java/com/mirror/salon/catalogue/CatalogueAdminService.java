package com.mirror.salon.catalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogueAdminService {

    private final SalonServiceRepository salonServiceRepository;
    private final ProductRepository productRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ProductCategoryRepository productCategoryRepository;

    public static class CatalogueException extends RuntimeException {
        public CatalogueException(String message) { super(message); }
    }

    public SalonService createService(String name, String description, BigDecimal price, int durationMins,
                                       Integer categoryId, boolean restricted) {
        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        SalonService s = new SalonService();
        s.setServiceName(name);
        s.setDescription(description);
        s.setPrice(price);
        s.setDurationMins(durationMins);
        s.setCategory(category);
        s.setRestricted(restricted);
        s.setActive(true);
        return salonServiceRepository.save(s);
    }

    public void updateService(Integer serviceId, String name, String description, BigDecimal price, int durationMins,
                               Integer categoryId, boolean restricted, boolean active) {
        SalonService s = salonServiceRepository.findById(serviceId)
                .orElseThrow(() -> new CatalogueException("Service not found."));
        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        s.setServiceName(name);
        s.setDescription(description);
        s.setPrice(price);
        s.setDurationMins(durationMins);
        s.setCategory(category);
        s.setRestricted(restricted);
        s.setActive(active);
        salonServiceRepository.save(s);
    }

    public Product createProduct(String name, String brand, String size, String description, BigDecimal price,
                                  int stockQty, Integer categoryId) {
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        Product p = new Product();
        p.setProductName(name);
        p.setBrand(brand);
        p.setSize(size);
        p.setDescription(description);
        p.setPrice(price);
        p.setStockQty(stockQty);
        p.setCategory(category);
        p.setActive(true);
        return productRepository.save(p);
    }

    public void updateProduct(Integer productId, String name, String brand, String size, String description,
                               BigDecimal price, int stockQty, Integer categoryId, boolean active) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new CatalogueException("Product not found."));
        ProductCategory category = productCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        p.setProductName(name);
        p.setBrand(brand);
        p.setSize(size);
        p.setDescription(description);
        p.setPrice(price);
        p.setStockQty(stockQty);
        p.setCategory(category);
        p.setActive(active);
        productRepository.save(p);
    }

    // ---- Categories ----

    public ServiceCategory createServiceCategory(String name, String description) {
        ServiceCategory c = new ServiceCategory();
        c.setCategoryName(name);
        c.setDescription(description);
        return serviceCategoryRepository.save(c);
    }

    public void updateServiceCategory(Integer id, String name, String description) {
        ServiceCategory c = serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        c.setCategoryName(name);
        c.setDescription(description);
        serviceCategoryRepository.save(c);
    }

    public void deleteServiceCategory(Integer id) {
        if (salonServiceRepository.countByCategory_CategoryId(id) > 0) {
            throw new CatalogueException("Can't delete — services still use this category.");
        }
        serviceCategoryRepository.deleteById(id);
    }

    public ProductCategory createProductCategory(String name, String description) {
        ProductCategory c = new ProductCategory();
        c.setCategoryName(name);
        c.setDescription(description);
        return productCategoryRepository.save(c);
    }

    public void updateProductCategory(Integer id, String name, String description) {
        ProductCategory c = productCategoryRepository.findById(id)
                .orElseThrow(() -> new CatalogueException("Category not found."));
        c.setCategoryName(name);
        c.setDescription(description);
        productCategoryRepository.save(c);
    }

    public void deleteProductCategory(Integer id) {
        if (productRepository.countByCategory_CategoryId(id) > 0) {
            throw new CatalogueException("Can't delete — products still use this category.");
        }
        productCategoryRepository.deleteById(id);
    }
}
