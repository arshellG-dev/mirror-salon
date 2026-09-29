package com.mirror.salon.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Integer> {

    List<ServiceCategory> findAllByOrderByCategoryId();
}
