package com.mirror.salon.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StylistServiceRepository extends JpaRepository<StylistService, StylistServiceId> {

    List<StylistService> findByStylist_StaffId(Integer stylistId);

    List<StylistService> findByService_ServiceId(Integer serviceId);

    boolean existsByStylist_StaffIdAndService_ServiceId(Integer stylistId, Integer serviceId);

    void deleteByStylist_StaffIdAndService_ServiceId(Integer stylistId, Integer serviceId);
}
