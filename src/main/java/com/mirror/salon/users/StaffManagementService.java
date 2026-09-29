package com.mirror.salon.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffManagementService {

    private final StaffRepository staffRepository;
    private final StylistRepository stylistRepository;
    private final PasswordEncoder passwordEncoder;

    public static class StaffException extends RuntimeException {
        public StaffException(String message) { super(message); }
    }

    @Transactional(readOnly = true)
    public List<Staff> listAll() {
        return staffRepository.findAll();
    }

    public Staff create(String firstName, String lastName, String mobileNo, String username,
                         String rawPassword, Staff.StaffType staffType, String bio, String specialtyTitle) {
        if (staffType == Staff.StaffType.ADMIN
                && staffRepository.findAll().stream().anyMatch(s -> s.getStaffType() == Staff.StaffType.ADMIN)) {
            throw new StaffException("Only one ADMIN (the owner) is allowed.");
        }
        if (staffRepository.findByUsername(username).isPresent()) {
            throw new StaffException("That username is already taken.");
        }

        Staff staff = new Staff();
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setMobileNo(mobileNo);
        staff.setUsername(username);
        staff.setPasswordHash(passwordEncoder.encode(rawPassword));
        staff.setStaffType(staffType);
        staff.setJoinedDate(LocalDate.now());
        staff.setActive(true);
        staffRepository.save(staff);

        if (staffType == Staff.StaffType.STYLIST) {
            Stylist stylist = new Stylist();
            stylist.setStaffId(staff.getStaffId());
            stylist.setStaff(staff);
            stylist.setBio(bio);
            stylist.setSpecialtyTitle(specialtyTitle);
            stylistRepository.save(stylist);
        }
        return staff;
    }

    public void setActive(Integer staffId, boolean active) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffException("Staff not found."));
        staff.setActive(active);
        staffRepository.save(staff);
    }

    public void resetPassword(Integer staffId, String rawPassword) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffException("Staff not found."));
        staff.setPasswordHash(passwordEncoder.encode(rawPassword));
        staffRepository.save(staff);
    }

    public void updateStylistProfile(Integer staffId, String bio, String specialtyTitle, String photoUrl) {
        Stylist stylist = stylistRepository.findById(staffId)
                .orElseThrow(() -> new StaffException("Stylist not found."));
        stylist.setBio(bio);
        stylist.setSpecialtyTitle(specialtyTitle);
        if (photoUrl != null) {
            stylist.setPhotoUrl(photoUrl);
        }
        stylistRepository.save(stylist);
    }
}
