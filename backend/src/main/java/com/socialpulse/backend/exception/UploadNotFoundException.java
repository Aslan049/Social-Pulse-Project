package com.socialpulse.backend.exception;

public class UploadNotFoundException extends RuntimeException {
    public UploadNotFoundException() {
        super("Yükleme bulunamadı");
    }
}
