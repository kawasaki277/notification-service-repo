package com.clinicbooking.notification.application.port.out;

import com.clinicbooking.notification.domain.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findAll();
}
