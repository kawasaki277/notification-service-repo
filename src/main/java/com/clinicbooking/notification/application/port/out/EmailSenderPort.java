package com.clinicbooking.notification.application.port.out;

public interface EmailSenderPort {
    /** @throws com.clinicbooking.notification.domain.DeliveryException si el proveedor rechaza o no responde */
    void send(String to, String subject, String body);
}
