package com.mirror.salon.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByScheduledAtDesc();

    boolean existsByBookingIdAndNotifType(Integer bookingId, Notification.NotifType notifType);
}
