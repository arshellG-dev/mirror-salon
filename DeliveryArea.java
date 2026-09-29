package com.mirror.salon.catalogue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "DELIVERY_AREA")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "area_name", length = 60, nullable = false, unique = true)
    private String areaName;

    @Column(name = "delivery_fee", nullable = false, precision = 8, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
