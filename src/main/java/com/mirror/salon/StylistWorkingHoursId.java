package com.mirror.salon.booking;

import java.io.Serializable;
import java.util.Objects;

public class StylistWorkingHoursId implements Serializable {

    private Integer stylist;
    private Integer dayOfWeek;

    public StylistWorkingHoursId() {}

    public StylistWorkingHoursId(Integer stylist, Integer dayOfWeek) {
        this.stylist = stylist;
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StylistWorkingHoursId that)) return false;
        return Objects.equals(stylist, that.stylist) && Objects.equals(dayOfWeek, that.dayOfWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stylist, dayOfWeek);
    }
}
