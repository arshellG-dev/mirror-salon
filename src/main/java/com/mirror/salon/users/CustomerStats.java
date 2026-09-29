package com.mirror.salon.users;

import java.time.LocalDate;

public record CustomerStats(Integer customerId, String fullName, String mobileNo, String adminNotes,
                             LocalDate firstVisitDate, Long totalVisits) {
}
