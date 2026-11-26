package com.clinicbooking.notification.infrastructure.config;

import com.clinicbooking.notification.application.port.in.NotificationUseCase;
import com.clinicbooking.notification.application.port.out.EmailSenderPort;
import com.clinicbooking.notification.application.port.out.NotificationRepositoryPort;
import com.clinicbooking.notification.application.port.out.SmsSenderPort;
import com.clinicbooking.notification.application.service.NotificationService;
import com.clinicbooking.notification.infrastructure.out.email.BrevoEmailAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class BeanConfig {

    @Bean
    Clock clock(@Value("${clinic.timezone:America/Bogota}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    EmailSenderPort emailSender(RestClient.Builder builder,
            @Value("${notification.email.base-url}") String url,
            @Value("${notification.email.api-key}") String apiKey,
            @Value("${notification.email.from-email}") String fromEmail,
            @Value("${notification.email.from-name}") String fromName,
            @Value("${notification.http.connect-timeout-ms}") long connectMs,
            @Value("${notification.http.read-timeout-ms}") long readMs) {
        return new BrevoEmailAdapter(RestClients.create(builder, url, connectMs, readMs), apiKey, fromEmail, fromName);
    }

    @Bean
    SmsSenderPort smsSender(RestClient.Builder builder,
            @Value("${notification.sms.base-url}") String url,
            @Value("${notification.sms.api-key}") String apiKey,
            @Value("${notification.sms.device-id:}") String deviceId,
            @Value("${notification.http.connect-timeout-ms}") long connectMs,
            @Value("${notification.http.read-timeout-ms}") long readMs) {
        return new com.clinicbooking.notification.infrastructure.out.sms.TextbeeSmsAdapter(builder, url, apiKey, deviceId, connectMs, readMs);
    }

    @Bean
    NotificationUseCase notificationUseCase(NotificationRepositoryPort repository, EmailSenderPort email,
                                            SmsSenderPort sms, Clock clock) {
        return new NotificationService(repository, email, sms, clock);
    }
}
