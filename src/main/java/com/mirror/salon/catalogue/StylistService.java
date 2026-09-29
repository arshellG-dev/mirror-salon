package com.mirror.salon.catalogue;

import com.mirror.salon.users.Stylist;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "STYLIST_SERVICE")
@IdClass(StylistServiceId.class)
@Getter
@Setter
@NoArgsConstructor
public class StylistService {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id")
    private Stylist stylist;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private SalonService service;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    public StylistService(Stylist stylist, SalonService service, LocalDate assignedDate) {
        this.stylist = stylist;
        this.service = service;
        this.assignedDate = assignedDate;
    }
}
