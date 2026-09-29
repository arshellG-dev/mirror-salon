package com.mirror.salon.users;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staffRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Staff staff = staffRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No staff account with username: " + username));

        if (!staff.isActive()) {
            throw new UsernameNotFoundException("Staff account is deactivated: " + username);
        }

        return User.withUsername(staff.getUsername())
                .password(staff.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + staff.getStaffType().name()))
                .build();
    }
}
