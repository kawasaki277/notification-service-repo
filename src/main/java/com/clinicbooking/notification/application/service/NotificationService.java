package com.clinicbooking.notification.application.service;

import com.clinicbooking.notification.application.port.in.NotificationUseCase;
import com.clinicbooking.notification.application.port.out.EmailSenderPort;
import com.clinicbooking.notification.application.port.out.NotificationRepositoryPort;
import com.clinicbooking.notification.application.port.out.SmsSenderPort;
import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.ChannelStats;
import com.clinicbooking.notification.domain.DeliveryException;
import com.clinicbooking.notification.domain.DomainException;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import com.clinicbooking.notification.domain.NotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NotificationService implements NotificationUseCase {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationRepositoryPort repository;
    private final EmailSenderPort emailSender;
    private final SmsSenderPort smsSender;
    private final Clock clock;

    public NotificationService(NotificationRepositoryPort repository, EmailSenderPort emailSender,
                               SmsSenderPort smsSender, Clock clock) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.clock = clock;
    }

    @Override
    public Notification sendEmail(String to, String subject, String body, Long appointmentId) {
        return deliver(Notification.create(Channel.EMAIL, to, subject, body, appointmentId, now()));
    }

    @Override
    public Notification sendSms(String to, String body, Long appointmentId) {
        return deliver(Notification.create(Channel.SMS, to, null, body, appointmentId, now()));
    }

    @Override
    public List<Notification> dispatchReminder(ReminderCommand c) {
        if (!c.appointmentStart().isAfter(now())) {
            throw new DomainException("La cita ya ocurrió; no se envía recordatorio");
        }
        String body = "Hola " + c.patientName() + ", le recordamos su cita médica el "
                + c.appointmentStart().format(FORMAT) + ". Si no puede asistir, cancele o reprograme con anticipación.";
        List<Notification> sent = new ArrayList<>();
        if (hasText(c.email())) {
            sent.add(sendEmail(c.email(), "Recordatorio de cita médica", body, c.appointmentId()));
        }
        if (hasText(c.phone())) {
            sent.add(sendSms(c.phone(), body, c.appointmentId()));
        }
        if (sent.isEmpty()) {
            throw new DomainException("El paciente no tiene correo ni teléfono para enviar el recordatorio");
        }
        return sent;
    }

    @Override
    public Map<Channel, ChannelStats> deliveryStats() {
        List<Notification> all = repository.findAll();
        Map<Channel, ChannelStats> stats = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            List<Notification> ofChannel = all.stream().filter(n -> n.channel() == channel).toList();
            long sent = ofChannel.stream().filter(n -> n.status() == NotificationStatus.SENT).count();
            long failed = ofChannel.stream().filter(n -> n.status() == NotificationStatus.FAILED).count();
            long retries = ofChannel.stream().filter(n -> n.attempts() > 1).mapToLong(n -> n.attempts() - 1L).sum();
            stats.put(channel, new ChannelStats(ofChannel.size(), sent, failed, retries));
        }
        return stats;
    }

    @Override
    public Notification retry(Long id) {
        Notification n = get(id);
        if (!n.canRetry()) {
            throw new DomainException("La notificación no admite reintentos (estado " + n.status()
                    + ", intentos " + n.attempts() + "/" + Notification.MAX_ATTEMPTS + ")");
        }
        return deliver(n);
    }

    @Override
    public Notification get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Notificación no encontrada: " + id));
    }

    /** Intenta la entrega; un fallo del proveedor queda registrado como FAILED (reintentable). */
    private Notification deliver(Notification n) {
        try {
            if (n.channel() == Channel.EMAIL) {
                emailSender.send(n.recipient(), n.subject(), n.body());
            } else {
                smsSender.send(n.recipient(), n.body());
            }
            n.markSent(now());
        } catch (DeliveryException e) {
            n.markFailed(e.getMessage());
        }
        return repository.save(n);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
