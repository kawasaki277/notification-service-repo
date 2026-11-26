package com.clinicbooking.notification.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "not_notifications")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String channel;
    public String recipient;
    public String subject;
    @Column(length = 2000)
    public String body;
    public Long appointmentId;
    public String status;
    public int attempts;
    @Column(length = 1000)
    public String lastError;
    public LocalDateTime createdAt;
    public LocalDateTime sentAt;
}
