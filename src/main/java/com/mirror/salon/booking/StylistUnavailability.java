package com.mirror.salon.booking;

import com.mirror.salon.users.Stylist;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "STYLIST_UNAVAILABILITY")
@IdClass(StylistUnavailabilityId.class)
@Getter
@Setter
@NoArgsConstructor
public class StylistUnavailability {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id")
    private Stylist stylist;

    @Id
    @Column(name = "block_date")
    private LocalDate blockDate;

    @Id
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "reason", length = 150)
    private String reason;
}
