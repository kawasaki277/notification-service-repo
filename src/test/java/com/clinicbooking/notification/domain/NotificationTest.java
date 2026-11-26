package com.clinicbooking.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);

    private Notification pending() {
        return Notification.create(Channel.EMAIL, "ana@mail.com", "Asunto", "Cuerpo", 1L, NOW);
    }

    @Test
    void createStartsPending() {
        Notification n = pending();
        assertEquals(NotificationStatus.PENDING, n.status());
        assertEquals(0, n.attempts());
        assertFalse(n.canRetry());
        n.assignId(3L);
        assertEquals(3L, n.id());
        assertEquals("Asunto", n.subject());
        assertEquals(1L, n.appointmentId());
        assertEquals(NOW, n.createdAt());
    }

    @Test
    void createValidatesRecipientAndBody() {
        assertThrows(DomainException.class, () -> Notification.create(Channel.SMS, " ", null, "x", null, NOW));
        assertThrows(DomainException.class, () -> Notification.create(Channel.SMS, "300", null, "", null, NOW));
    }

    @Test
    void markSentClearsError() {
        Notification n = pending();
        n.markFailed("timeout");
        n.markSent(NOW.plusMinutes(1));
        assertEquals(NotificationStatus.SENT, n.status());
        assertEquals(2, n.attempts());
        assertNull(n.lastError());
        assertEquals(NOW.plusMinutes(1), n.sentAt());
        assertFalse(n.canRetry());
    }

    @Test
    void retriesAreLimited() {
        Notification n = pending();
        n.markFailed("e1");
        assertTrue(n.canRetry());
        n.markFailed("e2");
        assertTrue(n.canRetry());
        n.markFailed("e3");
        assertFalse(n.canRetry());
        assertEquals("e3", n.lastError());
        assertEquals(Channel.EMAIL, n.channel());
        assertEquals("ana@mail.com", n.recipient());
        assertEquals("Cuerpo", n.body());
    }
}
