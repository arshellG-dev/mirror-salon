package com.mirror.salon.booking;

import com.mirror.salon.users.Stylist;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "STYLIST_WORKING_HOURS")
@IdClass(StylistWorkingHoursId.class)
@Getter
@Setter
@NoArgsConstructor
public class StylistWorkingHours {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stylist_id")
    private Stylist stylist;

    @Id
    @Column(name = "day_of_week", columnDefinition = "TINYINT UNSIGNED")
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public StylistWorkingHours(Stylist stylist, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.stylist = stylist;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
