package com.clinicbooking.notification.infrastructure.out;

import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import com.clinicbooking.notification.infrastructure.out.persistence.NotificationEntity;
import com.clinicbooking.notification.infrastructure.out.persistence.NotificationJpaRepository;
import com.clinicbooking.notification.infrastructure.out.persistence.NotificationPersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);

    @Mock NotificationJpaRepository repository;

    @Test
    void saveTruncatesLongErrorsAndMapsBack() {
        when(repository.save(any(NotificationEntity.class))).thenAnswer(i -> {
            NotificationEntity e = i.getArgument(0);
            e.id = 4L;
            return e;
        });
        NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository);
        Notification n = Notification.create(Channel.SMS, "300", null, "texto", 1L, NOW);
        n.markFailed("x".repeat(1500));
        Notification saved = adapter.save(n);
        assertEquals(4L, saved.id());
        assertEquals(1000, saved.lastError().length());
        assertEquals(NotificationStatus.FAILED, saved.status());

        Notification ok = Notification.create(Channel.EMAIL, "a@b.c", "s", "texto", null, NOW);
        ok.markSent(NOW);
        assertEquals(NotificationStatus.SENT, adapter.save(ok).status());
    }

    @Test
    void findByIdAndFindAll() {
        NotificationEntity e = new NotificationEntity();
        e.id = 1L;
        e.channel = "EMAIL";
        e.recipient = "a@b.c";
        e.body = "b";
        e.status = "SENT";
        e.attempts = 1;
        e.createdAt = NOW;
        when(repository.findById(1L)).thenReturn(Optional.of(e));
        when(repository.findAll()).thenReturn(List.of(e));
        NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository);
        assertTrue(adapter.findById(1L).isPresent());
        assertTrue(adapter.findById(2L).isEmpty());
        assertEquals(1, adapter.findAll().size());
    }
}
