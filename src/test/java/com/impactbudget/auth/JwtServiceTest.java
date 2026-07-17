package com.impactbudget.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-0123456789";

    private JwtService service(Duration ttl) {
        return new JwtService(new JwtProperties(SECRET, ttl));
    }

    private AppUser user() {
        AppUser u = new AppUser();
        u.setId(UUID.randomUUID());
        u.setEmail("a@b.com");
        return u;
    }

    @Test
    void issuedTokenVerifiesToItsSubject() {
        JwtService jwt = service(Duration.ofHours(1));
        AppUser u = user();

        String token = jwt.issue(u);

        assertThat(jwt.verify(token)).contains(u.getId().toString());
    }

    @Test
    void tamperedTokenFailsVerification() {
        JwtService jwt = service(Duration.ofHours(1));
        String token = jwt.issue(user());

        // Mutate the payload segment so the signature no longer matches.
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "AAAA" + "." + parts[2];

        assertThat(jwt.verify(tampered)).isEmpty();
        assertThat(jwt.verify("not.a.jwt")).isEmpty();
        assertThat(jwt.verify("")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = service(Duration.ofHours(1)).issue(user());
        JwtService other = new JwtService(
                new JwtProperties("a-totally-different-secret-also-long-enough-9876543210", Duration.ofHours(1)));

        assertThat(other.verify(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = service(Duration.ofSeconds(-1));   // already expired
        String token = jwt.issue(user());

        assertThat(jwt.verify(token)).isEmpty();
    }
}
