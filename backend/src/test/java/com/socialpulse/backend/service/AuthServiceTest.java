package com.socialpulse.backend.service;

import com.socialpulse.backend.auth.JwtService;
import com.socialpulse.backend.dto.*;
import com.socialpulse.backend.entity.User;
import com.socialpulse.backend.exception.EmailAlreadyExistsException;
import com.socialpulse.backend.exception.InvalidCredentialsException;
import com.socialpulse.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void register_savesNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Sifre12345")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        AuthResponse response = authService.register(
                new RegisterRequest("  Ali@Example.COM ", "Sifre12345", " Ali Veli "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("ali@example.com", captor.getValue().getEmail());
        assertEquals("hashed", captor.getValue().getPasswordHash());
        assertEquals("Ali Veli", captor.getValue().getFullName());
        assertEquals("token", response.token());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmail("ali@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(
                new RegisterRequest("ali@example.com", "Sifre12345", "Ali")));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void login_success_returnsToken() {
        User user = new User();
        user.setEmail("ali@example.com");
        user.setPasswordHash("hashed");
        user.setFullName("Ali");
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Sifre12345", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token");

        AuthResponse response = authService.login(new LoginRequest("ali@example.com", "Sifre12345"));

        assertEquals("token", response.token());
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("ali@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("yanlis", "hashed")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("ali@example.com", "yanlis")));
    }

    @Test
    void login_unknownEmail_throwsSameException() {
        when(userRepository.findByEmail("yok@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("yok@example.com", "Sifre12345")));
    }
}
