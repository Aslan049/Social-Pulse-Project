package com.socialpulse.backend.auth;

/** JWT'den çıkarılan, istek boyunca kullanılan kimlik bilgisi. */
public record AuthenticatedUser(Long id, String email) {}
