package com.tontinepro.tontinepro_backend.api.setup;

import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email:}")
    private String email;

    @Value("${app.superadmin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.warn("SUPERADMIN_EMAIL ou SUPERADMIN_PASSWORD non défini — compte super-admin ignoré");
            return;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Compte super-admin déjà existant : {}", email);
            return;
        }

        userRepository.save(User.builder()
                .email(email)
                .hashedPassword(passwordEncoder.encode(password))
                .role(User.Role.SUPER_ADMIN)
                .build());

        log.info("Compte super-admin créé : {}", email);
    }
}