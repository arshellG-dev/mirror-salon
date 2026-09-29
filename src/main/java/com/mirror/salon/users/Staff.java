package com.mirror.salon.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "STAFF")
@Getter
@Setter
@NoArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "mobile_no", unique = true, columnDefinition = "CHAR(10)")
    private String mobileNo;

    @Column(name = "username", length = 30, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false)
    private StaffType staffType;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public enum StaffType { ADMIN, STYLIST, RECEPTIONIST }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
