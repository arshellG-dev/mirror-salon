package com.mirror.salon.catalogue;

import com.mirror.salon.users.Stylist;
import com.mirror.salon.users.StylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StylistServiceManagementService {

    private final StylistServiceRepository stylistServiceRepository;
    private final StylistRepository stylistRepository;
    private final SalonServiceRepository salonServiceRepository;

    public static class ManagementException extends RuntimeException {
        public ManagementException(String message) { super(message); }
    }

    @Transactional(readOnly = true)
    public Set<Integer> myServiceIds(Integer stylistId) {
        return stylistServiceRepository.findByStylist_StaffId(stylistId).stream()
                .map(ss -> ss.getService().getServiceId())
                .collect(Collectors.toSet());
    }

    /** Stylists may freely toggle unrestricted services; restricted ones can only be assigned by admin. */
    public void setMyUnrestrictedServices(Integer stylistId, Set<Integer> desiredServiceIds) {
        Stylist stylist = stylistRepository.findById(stylistId)
                .orElseThrow(() -> new ManagementException("Stylist not found."));

        Set<Integer> currentIds = myServiceIds(stylistId);
        var unrestrictedServices = salonServiceRepository.findByActiveTrue().stream()
                .filter(s -> !s.isRestricted())
                .toList();

        for (SalonService service : unrestrictedServices) {
            boolean wants = desiredServiceIds.contains(service.getServiceId());
            boolean has = currentIds.contains(service.getServiceId());
            if (wants && !has) {
                stylistServiceRepository.save(new StylistService(stylist, service, LocalDate.now()));
            } else if (!wants && has) {
                stylistServiceRepository.deleteByStylist_StaffIdAndService_ServiceId(stylistId, service.getServiceId());
            }
        }
    }

    /** Admin-only: assign or unassign any service, including restricted ones. */
    public void adminSetStylistService(Integer stylistId, Integer serviceId, boolean assign) {
        Stylist stylist = stylistRepository.findById(stylistId)
                .orElseThrow(() -> new ManagementException("Stylist not found."));
        SalonService service = salonServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ManagementException("Service not found."));

        boolean has = stylistServiceRepository.existsByStylist_StaffIdAndService_ServiceId(stylistId, serviceId);
        if (assign && !has) {
            stylistServiceRepository.save(new StylistService(stylist, service, LocalDate.now()));
        } else if (!assign && has) {
            stylistServiceRepository.deleteByStylist_StaffIdAndService_ServiceId(stylistId, serviceId);
        }
    }
}
