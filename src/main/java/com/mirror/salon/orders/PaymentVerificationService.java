package com.mirror.salon.orders;

import com.mirror.salon.catalogue.Product;
import com.mirror.salon.catalogue.ProductRepository;
import com.mirror.salon.users.Notification;
import com.mirror.salon.users.NotificationRepository;
import com.mirror.salon.users.Staff;
import com.mirror.salon.users.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentVerificationService {

    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final StaffRepository staffRepository;
    private final NotificationRepository notificationRepository;

    public static class VerificationException extends RuntimeException {
        public VerificationException(String message) { super(message); }
    }

    @Transactional(readOnly = true)
    public List<Payment> pendingPayments() {
        return paymentRepository.findWithOrderByPaymentStatus(Payment.PaymentStatus.PENDING);
    }

    public void verify(Integer paymentId, Integer staffId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new VerificationException("Payment not found."));
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new VerificationException("This payment has already been processed.");
        }
        Staff staff = staffRepository.findById(staffId).orElseThrow();

        payment.setPaymentStatus(payment.getPaymentMethod() == Payment.PaymentMethod.COD
                ? Payment.PaymentStatus.COLLECTED : Payment.PaymentStatus.VERIFIED);
        payment.setVerifiedBy(staff);
        payment.setVerifiedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        ProductOrder order = payment.getOrder();
        order.setOrderStatus(ProductOrder.OrderStatus.CONFIRMED);

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQty(Math.max(0, product.getStockQty() - item.getQuantity()));
            productRepository.save(product);
        }

        queueNotification(order, Notification.NotifType.ORDER_STATUS,
                "Your order #" + order.getOrderId() + " payment has been confirmed and is being prepared.");
    }

    public void reject(Integer paymentId, Integer staffId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new VerificationException("A rejection reason is required.");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new VerificationException("Payment not found."));
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new VerificationException("This payment has already been processed.");
        }
        Staff staff = staffRepository.findById(staffId).orElseThrow();

        payment.setPaymentStatus(Payment.PaymentStatus.REJECTED);
        payment.setRejectionReason(reason);
        payment.setVerifiedBy(staff);
        payment.setVerifiedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        ProductOrder order = payment.getOrder();
        order.setOrderStatus(ProductOrder.OrderStatus.REJECTED);

        queueNotification(order, Notification.NotifType.PAYMENT_REJECTED,
                "Your order #" + order.getOrderId() + " payment was rejected: " + reason);
    }

    private void queueNotification(ProductOrder order, Notification.NotifType type, String message) {
        Notification notification = new Notification();
        notification.setCustomerId(order.getCustomer().getCustomerId());
        notification.setOrderId(order.getOrderId());
        notification.setNotifType(type);
        notification.setChannel(Notification.Channel.SMS);
        notification.setMessage(message);
        notification.setScheduledAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
