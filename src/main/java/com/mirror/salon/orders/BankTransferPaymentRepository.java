package com.mirror.salon.orders;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransferPaymentRepository extends JpaRepository<BankTransferPayment, Integer> {
}
