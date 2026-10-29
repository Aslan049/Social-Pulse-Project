package com.socialpulse.backend.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("E-posta veya parola hatalı");
    }
}
