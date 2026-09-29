package com.mirror.salon.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SalonServiceRepository extends JpaRepository<SalonService, Integer> {

    List<SalonService> findByActiveTrue();

    List<SalonService> findByActiveTrueAndCategory_CategoryId(Integer categoryId);

    List<SalonService> findByActiveTrueOrderByServiceId();

    List<SalonService> findByActiveTrueAndCategory_CategoryIdOrderByServiceId(Integer categoryId);

    @Query("SELECT s FROM SalonService s JOIN FETCH s.category ORDER BY s.serviceName")
    List<SalonService> findAllWithCategory();

    @Query("SELECT s FROM SalonService s JOIN FETCH s.category WHERE s.active = true ORDER BY s.serviceId")
    List<SalonService> findByActiveTrueWithCategoryOrderByServiceId();

    long countByCategory_CategoryId(Integer categoryId);
}
