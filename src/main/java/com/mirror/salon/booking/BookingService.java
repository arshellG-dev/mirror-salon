package com.mirror.salon.booking;

import com.mirror.salon.catalogue.SalonService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "BOOKING_SERVICE")
@IdClass(BookingServiceId.class)
@Getter
@Setter
@NoArgsConstructor
public class BookingService {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private SalonService service;

    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    @Column(name = "duration_at_booking", nullable = false, columnDefinition = "SMALLINT UNSIGNED")
    private int durationAtBooking;

    public BookingService(Booking booking, SalonService service, BigDecimal priceAtBooking, int durationAtBooking) {
        this.booking = booking;
        this.service = service;
        this.priceAtBooking = priceAtBooking;
        this.durationAtBooking = durationAtBooking;
    }
}
