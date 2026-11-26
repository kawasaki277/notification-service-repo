package com.clinicbooking.notification.domain;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(message);
    }
}
