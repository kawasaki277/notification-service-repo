package com.clinicbooking.notification.domain;

import java.time.LocalDateTime;

public class Notification {

    public static final int MAX_ATTEMPTS = 3;

    private Long id;
    private final Channel channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final Long appointmentId;
    private NotificationStatus status;
    private int attempts;
    private String lastError;
    private final LocalDateTime createdAt;
    private LocalDateTime sentAt;

    private Notification(Long id, Channel channel, String recipient, String subject, String body, Long appointmentId,
                         NotificationStatus status, int attempts, String lastError,
                         LocalDateTime createdAt, LocalDateTime sentAt) {
        this.id = id;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.appointmentId = appointmentId;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
    }

    public static Notification create(Channel channel, String recipient, String subject, String body,
                                      Long appointmentId, LocalDateTime now) {
        if (recipient == null || recipient.isBlank()) {
            throw new DomainException("El destinatario es obligatorio");
        }
        if (body == null || body.isBlank()) {
            throw new DomainException("El contenido es obligatorio");
        }
        return new Notification(null, channel, recipient, subject, body, appointmentId,
                NotificationStatus.PENDING, 0, null, now, null);
    }

    public static Notification restore(Long id, Channel channel, String recipient, String subject, String body,
                                       Long appointmentId, NotificationStatus status, int attempts, String lastError,
                                       LocalDateTime createdAt, LocalDateTime sentAt) {
        return new Notification(id, channel, recipient, subject, body, appointmentId, status, attempts,
                lastError, createdAt, sentAt);
    }

    public void markSent(LocalDateTime now) {
        attempts++;
        status = NotificationStatus.SENT;
        sentAt = now;
        lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        status = NotificationStatus.FAILED;
        lastError = error;
    }

    public boolean canRetry() {
        return status == NotificationStatus.FAILED && attempts < MAX_ATTEMPTS;
    }

    public void assignId(Long newId) {
        this.id = newId;
    }

    public Long id() { return id; }
    public Channel channel() { return channel; }
    public String recipient() { return recipient; }
    public String subject() { return subject; }
    public String body() { return body; }
    public Long appointmentId() { return appointmentId; }
    public NotificationStatus status() { return status; }
    public int attempts() { return attempts; }
    public String lastError() { return lastError; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime sentAt() { return sentAt; }
}
