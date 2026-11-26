package com.clinicbooking.notification.application;

import com.clinicbooking.notification.application.port.in.NotificationUseCase.ReminderCommand;
import com.clinicbooking.notification.application.port.out.EmailSenderPort;
import com.clinicbooking.notification.application.port.out.NotificationRepositoryPort;
import com.clinicbooking.notification.application.port.out.SmsSenderPort;
import com.clinicbooking.notification.application.service.NotificationService;
import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.ChannelStats;
import com.clinicbooking.notification.domain.DeliveryException;
import com.clinicbooking.notification.domain.DomainException;
import com.clinicbooking.notification.domain.NotFoundException;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);

    @Mock NotificationRepositoryPort repo;
    @Mock EmailSenderPort email;
    @Mock SmsSenderPort sms;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo, email, sms, CLOCK);
        when(repo.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            if (n.id() == null) {
                n.assignId(1L);
            }
            return n;
        });
    }

    @Test
    void sendEmailMarksSent() {
        Notification n = service.sendEmail("ana@mail.com", "Hola", "Cuerpo", 5L);
        assertEquals(NotificationStatus.SENT, n.status());
        assertEquals(Channel.EMAIL, n.channel());
        verify(email).send("ana@mail.com", "Hola", "Cuerpo");
    }

    @Test
    void providerFailureIsStoredAsFailedNotThrown() {
        doThrow(new DeliveryException("Brevo caído")).when(email).send(any(), any(), any());
        Notification n = service.sendEmail("ana@mail.com", "Hola", "Cuerpo", 5L);
        assertEquals(NotificationStatus.FAILED, n.status());
        assertEquals("Brevo caído", n.lastError());
        assertEquals(1, n.attempts());
    }

    @Test
    void sendSmsUsesSmsChannel() {
        Notification n = service.sendSms("3001234567", "Cita mañana", null);
        assertEquals(Channel.SMS, n.channel());
        verify(sms).send("3001234567", "Cita mañana");
        verify(email, never()).send(any(), any(), any());
    }

    @Test
    void reminderCombinesChannels() {
        List<Notification> sent = service.dispatchReminder(
                new ReminderCommand(1L, "Ana", "ana@mail.com", "300", NOW.plusHours(24)));
        assertEquals(2, sent.size());
        verify(email).send(any(), any(), any());
        verify(sms).send(any(), any());
    }

    @Test
    void reminderWithSingleChannel() {
        assertEquals(1, service.dispatchReminder(new ReminderCommand(1L, "Ana", null, "300", NOW.plusHours(24))).size());
        assertEquals(1, service.dispatchReminder(new ReminderCommand(1L, "Ana", "ana@mail.com", " ", NOW.plusHours(24))).size());
    }

    @Test
    void reminderValidations() {
        assertThrows(DomainException.class,
                () -> service.dispatchReminder(new ReminderCommand(1L, "Ana", "ana@mail.com", "300", NOW.minusHours(1))));
        assertThrows(DomainException.class,
                () -> service.dispatchReminder(new ReminderCommand(1L, "Ana", null, null, NOW.plusHours(24))));
    }

    @Test
    void retryResendsFailedNotification() {
        Notification failed = Notification.restore(9L, Channel.SMS, "300", null, "texto", 1L,
                NotificationStatus.FAILED, 1, "err", NOW, null);
        when(repo.findById(9L)).thenReturn(Optional.of(failed));
        Notification n = service.retry(9L);
        assertEquals(NotificationStatus.SENT, n.status());
        assertEquals(2, n.attempts());
    }

    @Test
    void retryRejectsNonRetryable() {
        Notification sent = Notification.restore(9L, Channel.SMS, "300", null, "texto", 1L,
                NotificationStatus.SENT, 1, null, NOW, NOW);
        Notification exhausted = Notification.restore(10L, Channel.SMS, "300", null, "texto", 1L,
                NotificationStatus.FAILED, 3, "err", NOW, null);
        when(repo.findById(9L)).thenReturn(Optional.of(sent));
        when(repo.findById(10L)).thenReturn(Optional.of(exhausted));
        assertThrows(DomainException.class, () -> service.retry(9L));
        assertThrows(DomainException.class, () -> service.retry(10L));
        assertThrows(NotFoundException.class, () -> service.retry(11L));
        assertThrows(NotFoundException.class, () -> service.get(11L));
    }

    @Test
    void deliveryStatsGroupsByChannel() {
        when(repo.findAll()).thenReturn(List.of(
                Notification.restore(1L, Channel.EMAIL, "a@b.c", "s", "b", null, NotificationStatus.SENT, 1, null, NOW, NOW),
                Notification.restore(2L, Channel.EMAIL, "a@b.c", "s", "b", null, NotificationStatus.SENT, 2, null, NOW, NOW),
                Notification.restore(3L, Channel.EMAIL, "a@b.c", "s", "b", null, NotificationStatus.FAILED, 3, "e", NOW, null),
                Notification.restore(4L, Channel.SMS, "300", null, "b", null, NotificationStatus.FAILED, 1, "e", NOW, null)));
        Map<Channel, ChannelStats> stats = service.deliveryStats();
        assertEquals(new ChannelStats(3, 2, 1, 3), stats.get(Channel.EMAIL));
        assertEquals(new ChannelStats(1, 0, 1, 0), stats.get(Channel.SMS));
    }
}
