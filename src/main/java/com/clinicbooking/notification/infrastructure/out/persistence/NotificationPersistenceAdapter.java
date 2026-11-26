package com.clinicbooking.notification.infrastructure.out.persistence;

import com.clinicbooking.notification.application.port.out.NotificationRepositoryPort;
import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.id = n.id();
        e.channel = n.channel().name();
        e.recipient = n.recipient();
        e.subject = n.subject();
        e.body = n.body();
        e.appointmentId = n.appointmentId();
        e.status = n.status().name();
        e.attempts = n.attempts();
        e.lastError = n.lastError() == null ? null : n.lastError().substring(0, Math.min(n.lastError().length(), 1000));
        e.createdAt = n.createdAt();
        e.sentAt = n.sentAt();
        return toDomain(repository.save(e));
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Notification> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private Notification toDomain(NotificationEntity e) {
        return Notification.restore(e.id, Channel.valueOf(e.channel), e.recipient, e.subject, e.body, e.appointmentId,
                NotificationStatus.valueOf(e.status), e.attempts, e.lastError, e.createdAt, e.sentAt);
    }
}
