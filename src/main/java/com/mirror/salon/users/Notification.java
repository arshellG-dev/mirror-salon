package com.mirror.salon.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "order_id")
    private Integer orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false)
    private NotifType notifType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Channel channel = Channel.SMS;

    @Column(name = "message", length = 320, nullable = false)
    private String message;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status", nullable = false)
    private SendStatus sendStatus = SendStatus.QUEUED;

    public enum NotifType {
        BOOKING_CONFIRMED, BOOKING_REMINDER, BOOKING_CANCELLED,
        BOOKING_RESCHEDULED, ORDER_PLACED, PAYMENT_REJECTED, ORDER_STATUS
    }

    public enum Channel { SMS }

    public enum SendStatus { QUEUED, SENT, FAILED }
}
