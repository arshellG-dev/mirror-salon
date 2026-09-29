package com.mirror.salon.catalogue;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCatalogService {

    private final ProductRepository productRepository;

    public List<Product> browse(Integer categoryId) {
        List<Product> products = categoryId == null
                ? productRepository.findByActiveTrue()
                : productRepository.findByActiveTrueAndCategory_CategoryId(categoryId);
        products.forEach(p -> p.getCategory().getCategoryName());
        return products;
    }

    public Product getDetail(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.getImages().size();
        return product;
    }
}
