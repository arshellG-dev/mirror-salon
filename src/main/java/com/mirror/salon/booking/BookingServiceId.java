package com.mirror.salon.booking;

import java.io.Serializable;
import java.util.Objects;

public class BookingServiceId implements Serializable {

    private Integer booking;
    private Integer service;

    public BookingServiceId() {}

    public BookingServiceId(Integer booking, Integer service) {
        this.booking = booking;
        this.service = service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookingServiceId that)) return false;
        return Objects.equals(booking, that.booking) && Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(booking, service);
    }
}
