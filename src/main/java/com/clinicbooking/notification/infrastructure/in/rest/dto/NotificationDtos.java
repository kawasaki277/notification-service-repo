package com.clinicbooking.notification.infrastructure.in.rest.dto;

import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record SendEmailRequest(@NotBlank @Email String to, @NotBlank String subject, @NotBlank String body,
                                   Long appointmentId) {
    }

    public record SendSmsRequest(@NotBlank String to, @NotBlank String body, Long appointmentId) {
    }

    public record ReminderRequest(@NotNull Long appointmentId, @NotBlank String patientName, String email,
                                  String phone, @NotNull LocalDateTime appointmentStart) {
    }

    public record NotificationResponse(Long id, Channel channel, String recipient, String subject,
                                       NotificationStatus status, int attempts, String lastError,
                                       Long appointmentId, LocalDateTime createdAt, LocalDateTime sentAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.id(), n.channel(), n.recipient(), n.subject(), n.status(),
                    n.attempts(), n.lastError(), n.appointmentId(), n.createdAt(), n.sentAt());
        }
    }
}
