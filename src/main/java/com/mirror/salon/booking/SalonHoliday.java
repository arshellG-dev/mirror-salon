package com.mirror.salon.booking;

import com.mirror.salon.users.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "SALON_HOLIDAY")
@Getter
@Setter
@NoArgsConstructor
public class SalonHoliday {

    @Id
    @Column(name = "holiday_date")
    private LocalDate holidayDate;

    @Column(name = "description", length = 100, nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declared_by", nullable = false)
    private Staff declaredBy;
}
