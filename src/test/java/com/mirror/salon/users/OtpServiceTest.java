package com.mirror.salon.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers OtpService's core business rule: an OTP can only be consumed once,
 * within its expiry window, matching the hashed code, and under the attempt
 * limit. This is the logic the whole customer login and booking-confirmation
 * flow depends on (see BookingController / BookingWorkflowService).
 */
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    private OtpService otpService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        otpService = new OtpService(otpVerificationRepository);
    }

    @Test
    void verifyReturnsSuccessWhenSubmittedCodeMatchesTheGeneratedOne() {
        ArgumentCaptor<OtpVerification> saved = ArgumentCaptor.forClass(OtpVerification.class);
        when(otpVerificationRepository.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        OtpService.GeneratedOtp generated = otpService.generate(1, null, OtpVerification.Purpose.LOGIN);
        OtpVerification storedRecord = saved.getValue();

        when(otpVerificationRepository.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(1, OtpVerification.Purpose.LOGIN))
                .thenReturn(Optional.of(storedRecord));

        OtpService.VerifyResult result = otpService.verify(1, OtpVerification.Purpose.LOGIN, generated.plainCode());

        assertThat(result).isEqualTo(OtpService.VerifyResult.SUCCESS);
        assertThat(storedRecord.getVerifiedAt()).isNotNull();
    }

    @Test
    void verifyReturnsInvalidCodeWhenSubmittedCodeIsWrong() {
        when(otpVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        OtpService.GeneratedOtp generated = otpService.generate(1, null, OtpVerification.Purpose.LOGIN);

        when(otpVerificationRepository.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(1, OtpVerification.Purpose.LOGIN))
                .thenReturn(Optional.of(generated.record()));

        OtpService.VerifyResult result = otpService.verify(1, OtpVerification.Purpose.LOGIN, "000000");

        assertThat(result).isEqualTo(OtpService.VerifyResult.INVALID_CODE);
        assertThat(generated.record().getAttemptCount()).isEqualTo(1);
    }

    @Test
    void verifyReturnsExpiredOrLockedWhenTheOtpHasAlreadyExpired() {
        OtpVerification expired = new OtpVerification();
        expired.setCustomerId(1);
        expired.setPurpose(OtpVerification.Purpose.LOGIN);
        expired.setOtpHash("irrelevant");
        expired.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        expired.setAttemptCount(0);

        when(otpVerificationRepository.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(1, OtpVerification.Purpose.LOGIN))
                .thenReturn(Optional.of(expired));

        OtpService.VerifyResult result = otpService.verify(1, OtpVerification.Purpose.LOGIN, "123456");

        assertThat(result).isEqualTo(OtpService.VerifyResult.EXPIRED_OR_LOCKED);
    }

    @Test
    void verifyReturnsExpiredOrLockedWhenNoOtpWasEverGenerated() {
        when(otpVerificationRepository.findTopByCustomerIdAndPurposeOrderByCreatedAtDesc(99, OtpVerification.Purpose.LOGIN))
                .thenReturn(Optional.empty());

        OtpService.VerifyResult result = otpService.verify(99, OtpVerification.Purpose.LOGIN, "123456");

        assertThat(result).isEqualTo(OtpService.VerifyResult.EXPIRED_OR_LOCKED);
    }
}
