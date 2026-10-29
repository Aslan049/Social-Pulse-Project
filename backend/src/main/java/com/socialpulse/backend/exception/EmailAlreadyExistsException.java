package com.socialpulse.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Bu e-posta adresi zaten kayıtlı: " + email);
    }
}
