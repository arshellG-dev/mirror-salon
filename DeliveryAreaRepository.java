package com.mirror.salon.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, Integer> {

    List<DeliveryArea> findByActiveTrue();
}
