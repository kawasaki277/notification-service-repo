package com.clinicbooking.notification.infrastructure.out;

import com.clinicbooking.notification.domain.DeliveryException;
import com.clinicbooking.notification.infrastructure.out.email.BrevoEmailAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProviderAdaptersTest {

    // ---------- Brevo ----------
    private final RestClient.Builder brevoBuilder = RestClient.builder().baseUrl("http://brevo.test");
    private final MockRestServiceServer brevoServer = MockRestServiceServer.bindTo(brevoBuilder).build();

    private BrevoEmailAdapter brevo(String key) {
        return new BrevoEmailAdapter(brevoBuilder.build(), key, "no-reply@clinic.com", "Clinic");
    }

    @Test
    void brevoSendsTransactionalEmail() {
        brevoServer.expect(requestTo("http://brevo.test/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "secret"))
                .andExpect(jsonPath("$.to[0].email").value("ana@mail.com"))
                .andExpect(jsonPath("$.subject").value("Hola"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{\"messageId\":\"1\"}"));
        brevo("secret").send("ana@mail.com", "Hola", "Cuerpo");
        brevoServer.verify();
    }

    @Test
    void brevoUsesDefaultSubjectWhenMissing() {
        brevoServer.expect(requestTo("http://brevo.test/v3/smtp/email"))
                .andExpect(jsonPath("$.subject").value("Notificación ClinicBooking"))
                .andRespond(withStatus(HttpStatus.CREATED));
        brevo("secret").send("ana@mail.com", null, "Cuerpo");
        brevoServer.verify();
    }

    @Test
    void brevoWrapsProviderErrors() {
        brevoServer.expect(requestTo("http://brevo.test/v3/smtp/email")).andRespond(withServerError());
        assertThrows(DeliveryException.class, () -> brevo("secret").send("ana@mail.com", "Hola", "Cuerpo"));
    }

    @Test
    void brevoSimulatesWithoutApiKey() {
        assertDoesNotThrow(() -> brevo("").send("ana@mail.com", "Hola", "Cuerpo"));
        assertDoesNotThrow(() -> brevo(null).send("ana@mail.com", "Hola", "Cuerpo"));
        brevoServer.verify(); // no hubo llamadas HTTP
    }

}
