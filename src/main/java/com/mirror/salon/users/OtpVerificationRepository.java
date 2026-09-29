package com.mirror.salon.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(
            Integer customerId, OtpVerification.Purpose purpose);

    Optional<OtpVerification> findTopByBookingIdAndPurposeOrderByCreatedAtDesc(
            Integer bookingId, OtpVerification.Purpose purpose);
}
