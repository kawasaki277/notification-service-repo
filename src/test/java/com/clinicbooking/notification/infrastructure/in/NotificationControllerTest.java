package com.clinicbooking.notification.infrastructure.in;

import com.clinicbooking.notification.application.port.in.NotificationUseCase;
import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.ChannelStats;
import com.clinicbooking.notification.domain.Notification;
import com.clinicbooking.notification.domain.NotificationStatus;
import com.clinicbooking.notification.infrastructure.in.rest.NotificationController;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.NotificationResponse;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.ReminderRequest;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.SendEmailRequest;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.SendSmsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 8, 0);

    @Mock NotificationUseCase useCase;

    private Notification notification(Channel channel, NotificationStatus status) {
        return Notification.restore(1L, channel, "dest", "s", "b", 5L, status, 1, null, NOW, NOW);
    }

    @Test
    void sendEndpointsMapResponses() {
        NotificationController controller = new NotificationController(useCase, new com.clinicbooking.notification.infrastructure.out.sms.ClaveServicioValidator(""));
        when(useCase.sendEmail("a@b.c", "s", "b", 5L)).thenReturn(notification(Channel.EMAIL, NotificationStatus.SENT));
        when(useCase.sendSms("300", "b", 5L)).thenReturn(notification(Channel.SMS, NotificationStatus.FAILED));

        NotificationResponse email = controller.sendEmail(new SendEmailRequest("a@b.c", "s", "b", 5L));
        NotificationResponse sms = controller.sendSms(new SendSmsRequest("300", "b", 5L), null);
        assertEquals(NotificationStatus.SENT, email.status());
        assertEquals(Channel.SMS, sms.channel());
        assertEquals(NotificationStatus.FAILED, sms.status());
    }

    @Test
    void reminderStatsRetryAndGetDelegate() {
        NotificationController controller = new NotificationController(useCase, new com.clinicbooking.notification.infrastructure.out.sms.ClaveServicioValidator(""));
        when(useCase.dispatchReminder(any())).thenReturn(List.of(
                notification(Channel.EMAIL, NotificationStatus.SENT), notification(Channel.SMS, NotificationStatus.SENT)));
        Map<Channel, ChannelStats> stats = Map.of(Channel.EMAIL, new ChannelStats(1, 1, 0, 0));
        when(useCase.deliveryStats()).thenReturn(stats);
        when(useCase.retry(1L)).thenReturn(notification(Channel.SMS, NotificationStatus.SENT));
        when(useCase.get(1L)).thenReturn(notification(Channel.EMAIL, NotificationStatus.SENT));

        assertEquals(2, controller.dispatchReminder(new ReminderRequest(5L, "Ana", "a@b.c", "300", NOW.plusDays(1))).size());
        assertEquals(stats, controller.deliveryStats());
        assertEquals(NotificationStatus.SENT, controller.retry(1L).status());
        assertEquals(Channel.EMAIL, controller.get(1L).channel());
    }
}
