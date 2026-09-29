package com.mirror.salon.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueAndCategory_CategoryId(Integer categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category ORDER BY p.productName")
    List<Product> findAllWithCategory();

    long countByActiveTrueAndStockQtyLessThanEqual(int threshold);

    long countByCategory_CategoryId(Integer categoryId);
}
