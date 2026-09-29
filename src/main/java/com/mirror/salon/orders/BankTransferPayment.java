package com.mirror.salon.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "BANK_TRANSFER_PAYMENT")
@Getter
@Setter
@NoArgsConstructor
public class BankTransferPayment {

    @Id
    @Column(name = "payment_id")
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "slip_file_path", length = 255, nullable = false)
    private String slipFilePath;

    @Column(name = "bank_reference", length = 50)
    private String bankReference;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
