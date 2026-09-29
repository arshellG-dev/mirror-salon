package com.mirror.salon.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "COD_PAYMENT")
@Getter
@Setter
@NoArgsConstructor
public class CodPayment {

    @Id
    @Column(name = "payment_id")
    private Integer paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;
}
