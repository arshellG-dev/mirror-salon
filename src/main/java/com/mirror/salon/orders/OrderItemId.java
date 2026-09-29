package com.mirror.salon.orders;

import java.io.Serializable;
import java.util.Objects;

public class OrderItemId implements Serializable {

    private Integer order;
    private Integer lineNo;

    public OrderItemId() {}

    public OrderItemId(Integer order, Integer lineNo) {
        this.order = order;
        this.lineNo = lineNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId that)) return false;
        return Objects.equals(order, that.order) && Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, lineNo);
    }
}
