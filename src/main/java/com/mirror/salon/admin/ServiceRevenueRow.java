package com.mirror.salon.admin;

import java.math.BigDecimal;

public interface ServiceRevenueRow {
    String getMonth();
    String getServiceName();
    String getStylistName();
    BigDecimal getRevenue();
}
