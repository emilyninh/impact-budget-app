package com.impactbudget.auth;

import com.impactbudget.common.DemoIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dev-only demo account so reviewers can log in and see the seeded dashboard. Gated by
 * {@code demo.seed-enabled=true}. Runs first (@Order) so the demo user's UUID exists before
 * the transaction/goal/budget seeders attach data to it. Idempotent.
 */
@Component
@ConditionalOnProperty(name = "demo.seed-enabled", havingValue = "true")
@Order(5)
class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        UUID id = UUID.fromString(DemoIds.DEMO_USER_ID);
        if (userRepository.existsById(id)) {
            return;
        }
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail(DemoIds.DEMO_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(DemoIds.DEMO_PASSWORD));
        user.setDisplayName("Demo User");
        userRepository.save(user);
        log.info("Demo seed: demo account created ({})", DemoIds.DEMO_EMAIL);
    }
}
