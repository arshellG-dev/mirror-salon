package com.mirror.salon.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findByOrder_OrderId(Integer orderId);

    List<Payment> findByPaymentStatusOrderByPaymentIdAsc(Payment.PaymentStatus status);

    @Query("SELECT p FROM Payment p JOIN FETCH p.order o JOIN FETCH o.customer "
            + "WHERE p.paymentStatus = :status ORDER BY p.paymentId")
    List<Payment> findWithOrderByPaymentStatus(@Param("status") Payment.PaymentStatus status);

    long countByPaymentStatus(Payment.PaymentStatus status);
}
