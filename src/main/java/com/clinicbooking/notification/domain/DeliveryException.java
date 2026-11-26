package com.clinicbooking.notification.domain;

/** Fallo al entregar una notificación por un canal externo. */
public class DeliveryException extends RuntimeException {
    public DeliveryException(String message) {
        super(message);
    }

    public DeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
