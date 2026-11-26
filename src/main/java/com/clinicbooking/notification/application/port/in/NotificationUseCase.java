package com.clinicbooking.notification.application.port.in;

import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.ChannelStats;
import com.clinicbooking.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NotificationUseCase {

    Notification sendEmail(String to, String subject, String body, Long appointmentId);

    Notification sendSms(String to, String body, Long appointmentId);

    List<Notification> dispatchReminder(ReminderCommand command);

    Map<Channel, ChannelStats> deliveryStats();

    Notification retry(Long id);

    Notification get(Long id);

    record ReminderCommand(Long appointmentId, String patientName, String email, String phone,
                           LocalDateTime appointmentStart) {
    }
}
