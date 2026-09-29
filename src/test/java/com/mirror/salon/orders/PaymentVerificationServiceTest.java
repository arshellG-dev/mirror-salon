package com.mirror.salon.orders;

import com.mirror.salon.catalogue.ProductRepository;
import com.mirror.salon.users.Customer;
import com.mirror.salon.users.Notification;
import com.mirror.salon.users.NotificationRepository;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers PaymentVerificationService.reject(), the receptionist/admin flow
 * that turns down a bank-transfer slip: a reason is mandatory, and a
 * successful rejection must flip both the Payment and its parent
 * ProductOrder to their rejected states and queue a customer notification.
 */
@ExtendWith(MockitoExtension.class)
class PaymentVerificationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private NotificationRepository notificationRepository;

    private PaymentVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentVerificationService(paymentRepository, productRepository, staffRepository, notificationRepository);
    }

    @Test
    void rejectRequiresANonBlankReason() {
        assertThrows(PaymentVerificationService.VerificationException.class,
                () -> service.reject(1, 1, "   "));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void rejectMarksThePaymentAndOrderRejectedAndQueuesANotification() {
        Customer customer = new Customer();
        customer.setCustomerId(5);

        ProductOrder order = new ProductOrder();
        order.setOrderId(42);
        order.setCustomer(customer);
        order.setOrderStatus(ProductOrder.OrderStatus.PAYMENT_PENDING);

        Payment payment = new Payment();
        payment.setPaymentId(1);
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
        payment.setAmount(new BigDecimal("1500.00"));
        payment.setOrder(order);

        Staff staff = new Staff();
        staff.setStaffId(2);

        when(paymentRepository.findById(1)).thenReturn(Optional.of(payment));
        when(staffRepository.findById(2)).thenReturn(Optional.of(staff));

        service.reject(1, 2, "Slip unreadable");

        assertThat(payment.getPaymentStatus()).isEqualTo(Payment.PaymentStatus.REJECTED);
        assertThat(payment.getRejectionReason()).isEqualTo("Slip unreadable");
        assertThat(payment.getVerifiedBy()).isEqualTo(staff);
        assertThat(order.getOrderStatus()).isEqualTo(ProductOrder.OrderStatus.REJECTED);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void rejectRefusesAPaymentThatWasAlreadyProcessed() {
        Payment payment = new Payment();
        payment.setPaymentStatus(Payment.PaymentStatus.VERIFIED);
        when(paymentRepository.findById(1)).thenReturn(Optional.of(payment));

        assertThrows(PaymentVerificationService.VerificationException.class,
                () -> service.reject(1, 2, "too late"));

        verify(notificationRepository, never()).save(any());
    }
}
