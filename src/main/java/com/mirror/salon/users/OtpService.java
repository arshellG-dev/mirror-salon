package com.mirror.salon.users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpVerificationRepository;
    private final SecureRandom random = new SecureRandom();

    public record GeneratedOtp(OtpVerification record, String plainCode) {}

    /** Generates and persists a new OTP, returning the plain-text code for demo-mode display. */
    public GeneratedOtp generate(Integer customerId, Integer bookingId, OtpVerification.Purpose purpose) {
        String code = generateNumericCode();
        OtpVerification otp = new OtpVerification();
        otp.setCustomerId(customerId);
        otp.setBookingId(bookingId);
        otp.setPurpose(purpose);
        otp.setOtpHash(hash(code));
        LocalDateTime now = LocalDateTime.now();
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        otp.setAttemptCount(0);
        otpVerificationRepository.save(otp);
        return new GeneratedOtp(otp, code);
    }

    public enum VerifyResult { SUCCESS, INVALID_CODE, EXPIRED_OR_LOCKED, TOO_MANY_ATTEMPTS }

    public VerifyResult verify(Integer customerId, OtpVerification.Purpose purpose, String submittedCode) {
        Optional<OtpVerification> latest =
                otpVerificationRepository.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(customerId, purpose);

        if (latest.isEmpty()) {
            return VerifyResult.EXPIRED_OR_LOCKED;
        }

        OtpVerification otp = latest.get();

        if (otp.getVerifiedAt() != null || otp.getExpiresAt().isBefore(LocalDateTime.now())
                || otp.getAttemptCount() >= MAX_ATTEMPTS) {
            return VerifyResult.EXPIRED_OR_LOCKED;
        }

        if (!otp.getOtpHash().equals(hash(submittedCode))) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpVerificationRepository.save(otp);
            return otp.getAttemptCount() >= MAX_ATTEMPTS ? VerifyResult.TOO_MANY_ATTEMPTS : VerifyResult.INVALID_CODE;
        }

        otp.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otp);
        return VerifyResult.SUCCESS;
    }

    public VerifyResult verifyForBooking(Integer bookingId, String submittedCode) {
        Optional<OtpVerification> latest = otpVerificationRepository
                .findTopByBookingIdAndPurposeOrderByCreatedAtDesc(bookingId, OtpVerification.Purpose.BOOKING_CONFIRMATION);

        if (latest.isEmpty()) {
            return VerifyResult.EXPIRED_OR_LOCKED;
        }

        OtpVerification otp = latest.get();

        if (otp.getVerifiedAt() != null || otp.getExpiresAt().isBefore(LocalDateTime.now())
                || otp.getAttemptCount() >= MAX_ATTEMPTS) {
            return VerifyResult.EXPIRED_OR_LOCKED;
        }

        if (!otp.getOtpHash().equals(hash(submittedCode))) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpVerificationRepository.save(otp);
            return otp.getAttemptCount() >= MAX_ATTEMPTS ? VerifyResult.TOO_MANY_ATTEMPTS : VerifyResult.INVALID_CODE;
        }

        otp.setVerifiedAt(LocalDateTime.now());
        otpVerificationRepository.save(otp);
        return VerifyResult.SUCCESS;
    }

    private String generateNumericCode() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
