package com.clinicbooking.notification.infrastructure.in.rest;

import com.clinicbooking.notification.application.port.in.NotificationUseCase;
import com.clinicbooking.notification.application.port.in.NotificationUseCase.ReminderCommand;
import com.clinicbooking.notification.domain.Channel;
import com.clinicbooking.notification.domain.ChannelStats;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.NotificationResponse;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.ReminderRequest;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.SendEmailRequest;
import com.clinicbooking.notification.infrastructure.in.rest.dto.NotificationDtos.SendSmsRequest;
import com.clinicbooking.notification.infrastructure.out.sms.ClaveServicioValidator;
import org.springframework.web.bind.annotation.RequestHeader;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Los envíos responden 201 con el estado de la entrega (SENT / FAILED). Un FAILED no es un error HTTP:
 * queda registrado y puede reintentarse con POST /{id}/retry.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationUseCase useCase;
    private final ClaveServicioValidator claveValidator;

    public NotificationController(NotificationUseCase useCase, ClaveServicioValidator claveValidator) {
        this.useCase = useCase;
        this.claveValidator = claveValidator;
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable Long id) {
        return NotificationResponse.from(useCase.get(id));
    }

    // ---- APIs de negocio (no-CRUD) ----
    @PostMapping("/send-email")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse sendEmail(@Valid @RequestBody SendEmailRequest r) {
        return NotificationResponse.from(useCase.sendEmail(r.to(), r.subject(), r.body(), r.appointmentId()));
    }

    @PostMapping("/send-sms")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse sendSms(@Valid @RequestBody SendSmsRequest r, @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {
        claveValidator.validateIfConfigured(internalKey);
        return NotificationResponse.from(useCase.sendSms(r.to(), r.body(), r.appointmentId()));
    }

    @PostMapping("/dispatch-appointment-reminder")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NotificationResponse> dispatchReminder(@Valid @RequestBody ReminderRequest r) {
        return useCase.dispatchReminder(new ReminderCommand(r.appointmentId(), r.patientName(), r.email(),
                r.phone(), r.appointmentStart())).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/delivery-stats")
    public Map<Channel, ChannelStats> deliveryStats() {
        return useCase.deliveryStats();
    }

    @PostMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable Long id) {
        return NotificationResponse.from(useCase.retry(id));
    }
}
