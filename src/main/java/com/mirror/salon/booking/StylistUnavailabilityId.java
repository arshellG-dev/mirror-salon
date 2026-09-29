package com.mirror.salon.booking;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class StylistUnavailabilityId implements Serializable {

    private Integer stylist;
    private LocalDate blockDate;
    private LocalTime startTime;

    public StylistUnavailabilityId() {}

    public StylistUnavailabilityId(Integer stylist, LocalDate blockDate, LocalTime startTime) {
        this.stylist = stylist;
        this.blockDate = blockDate;
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StylistUnavailabilityId that)) return false;
        return Objects.equals(stylist, that.stylist) && Objects.equals(blockDate, that.blockDate)
                && Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stylist, blockDate, startTime);
    }
}
