package com.impactbudget.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/** Registration and login: BCrypt password handling + JWT issuance. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult register(String email, String rawPassword, String displayName) {
        String normalized = normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new AuthExceptions.EmailAlreadyUsedException(normalized);
        }
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail(normalized);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        userRepository.save(user);
        return toResult(user);
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmail(normalize(email))
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(AuthExceptions.InvalidCredentialsException::new);
        return toResult(user);
    }

    private AuthResult toResult(AppUser user) {
        return new AuthResult(jwtService.issue(user), jwtService.ttlSeconds(),
                user.getId().toString(), user.getEmail(), user.getDisplayName());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
