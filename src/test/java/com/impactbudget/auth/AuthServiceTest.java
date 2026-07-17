package com.impactbudget.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    PasswordEncoder encoder = new BCryptPasswordEncoder();
    JwtService jwtService = new JwtService(
            new JwtProperties("unit-test-secret-long-enough-for-hs256-0123456789", Duration.ofHours(1)));
    AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        when(userRepository.existsByEmail("new@user.com")).thenReturn(false);

        AuthResult result = service.register("New@User.com", "supersecret", "New");

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        org.mockito.Mockito.verify(userRepository).save(saved.capture());
        AppUser u = saved.getValue();
        assertThat(u.getEmail()).isEqualTo("new@user.com");                 // normalized to lowercase
        assertThat(u.getPasswordHash()).isNotEqualTo("supersecret");         // hashed, not plaintext
        assertThat(encoder.matches("supersecret", u.getPasswordHash())).isTrue();
        assertThat(jwtService.verify(result.token())).contains(u.getId().toString());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dupe@user.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("dupe@user.com", "supersecret", "Dupe"))
                .isInstanceOf(AuthExceptions.EmailAlreadyUsedException.class);
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("me@user.com");
        u.setPasswordHash(encoder.encode("correct-horse"));
        when(userRepository.findByEmail("me@user.com")).thenReturn(Optional.of(u));

        AuthResult result = service.login("Me@User.com", "correct-horse");

        assertThat(result.userId()).isEqualTo(u.getId().toString());
        assertThat(jwtService.verify(result.token())).isPresent();
    }

    @Test
    void loginFailsWithWrongPassword() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("me@user.com");
        u.setPasswordHash(encoder.encode("correct-horse"));
        when(userRepository.findByEmail("me@user.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.login("me@user.com", "wrong"))
                .isInstanceOf(AuthExceptions.InvalidCredentialsException.class);
    }

    @Test
    void loginFailsWhenUserMissing() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost@user.com", "whatever"))
                .isInstanceOf(AuthExceptions.InvalidCredentialsException.class);
    }
}
