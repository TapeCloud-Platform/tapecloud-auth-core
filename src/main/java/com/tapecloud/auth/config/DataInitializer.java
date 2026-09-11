package com.tapecloud.auth.config;

import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import com.tapecloud.auth.user.repository.RoleRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tapecloud.admin.emails:totosanchez2610@gmail.com,admin@tapecloud.com}")
    private String adminEmailsProperty;

    public DataInitializer(RoleRepository roleRepository, AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        List<String> adminEmails = Arrays.stream(adminEmailsProperty.split(","))
                .map(String::trim)
                .map(email -> email.toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .toList();

        for (String email : adminEmails) {
            userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(
                    existingUser -> {
                        if (existingUser.getRoles().stream().noneMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
                            existingUser.addRole(adminRole);
                            userRepository.save(existingUser);
                        }
                    },
                    () -> {
                        AppUser newAdmin = new AppUser(email, passwordEncoder.encode("admin123"));
                        newAdmin.addRole(userRole);
                        newAdmin.addRole(adminRole);
                        userRepository.save(newAdmin);
                    }
            );
        }
    }
}
