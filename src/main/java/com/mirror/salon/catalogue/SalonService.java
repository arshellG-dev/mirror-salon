package com.mirror.salon.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SERVICE")
@Getter
@Setter
@NoArgsConstructor
public class SalonService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Integer serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ServiceCategory category;

    @Column(name = "service_name", length = 100, nullable = false, unique = true)
    private String serviceName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_mins", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int durationMins;

    @Column(name = "is_restricted", nullable = false)
    private boolean restricted;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToMany
    @JoinTable(
        name = "SERVICE_PRODUCT",
        joinColumns = @JoinColumn(name = "service_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> productsUsed = new HashSet<>();
}
