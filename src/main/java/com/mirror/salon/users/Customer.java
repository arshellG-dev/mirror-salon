package com.mirror.salon.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "CUSTOMER")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "mobile_no", nullable = false, unique = true, columnDefinition = "CHAR(10)")
    private String mobileNo;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_via", nullable = false)
    private CreatedVia createdVia = CreatedVia.ONLINE;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    public enum CreatedVia { ONLINE, WALK_IN, ADMIN }
}
