package com.clinicbooking.notification.infrastructure.out.email;

import com.clinicbooking.notification.application.port.out.EmailSenderPort;
import com.clinicbooking.notification.domain.DeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/** API externa #1 (correo): Brevo transactional email (capa gratuita). Sin API key => modo simulado. */
public class BrevoEmailAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailAdapter.class);

    private final RestClient client;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailAdapter(RestClient client, String apiKey, String fromEmail, String fromName) {
        this.client = client;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("[SIMULADO] correo a {} | {}", to, subject);
            return;
        }
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject == null ? "Notificación ClinicBooking" : subject,
                "textContent", body);
        try {
            client.post().uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new DeliveryException("Brevo: " + e.getMessage(), e);
        }
    }
}
