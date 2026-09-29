package com.mirror.salon.catalogue;

import java.io.Serializable;
import java.util.Objects;

public class StylistServiceId implements Serializable {

    private Integer stylist;
    private Integer service;

    public StylistServiceId() {}

    public StylistServiceId(Integer stylist, Integer service) {
        this.stylist = stylist;
        this.service = service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StylistServiceId that)) return false;
        return Objects.equals(stylist, that.stylist) && Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stylist, service);
    }
}
