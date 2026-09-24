package com.tapecloud.auth.config;

import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import com.tapecloud.auth.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo garantiza que existan los roles base. Ya NO crea administradores automáticamente
 * ni con una contraseña fija (ver hallazgo S-01 de la auditoría de seguridad): el alta del
 * primer admin es un paso explícito, único, fuera de este arranque automático (ver README).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Sin valor por defecto: si no se completan ambas variables, no se crea ningún admin.
    @Value("${tapecloud.admin.bootstrap-email:}")
    private String bootstrapEmail;

    @Value("${tapecloud.admin.bootstrap-password:}")
    private String bootstrapPassword;

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

        if (userRepository.existsByRoleName("ROLE_ADMIN")) {
            // Ya hay al menos un admin: no se vuelve a tocar (evita restaurar un rol revocado a propósito).
            return;
        }

        if (bootstrapEmail == null || bootstrapEmail.isBlank() || bootstrapPassword == null || bootstrapPassword.isBlank()) {
            log.warn("No hay ninguna cuenta con ROLE_ADMIN y no se configuraron "
                    + "ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD: no se crea ningún admin automáticamente. "
                    + "Configurá esas variables una sola vez para dar de alta el primer admin, o asigná el rol "
                    + "manualmente en la base.");
            return;
        }
        if (bootstrapPassword.trim().length() < 12) {
            log.warn("ADMIN_BOOTSTRAP_PASSWORD tiene menos de 12 caracteres: no se crea ningún admin. "
                    + "Usá una contraseña más larga.");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(bootstrapEmail)) {
            log.warn("ADMIN_BOOTSTRAP_EMAIL ya existe como usuario sin ROLE_ADMIN; no se modifica automáticamente. "
                    + "Usá /api/admin/users/{id}/grant-admin con una cuenta admin existente.");
            return;
        }

        AppUser admin = new AppUser(bootstrapEmail.trim().toLowerCase(java.util.Locale.ROOT), passwordEncoder.encode(bootstrapPassword));
        admin.addRole(userRole);
        admin.addRole(adminRole);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.warn("Se creó la cuenta admin inicial para {} a partir de ADMIN_BOOTSTRAP_EMAIL/PASSWORD. "
                + "Cambiá la contraseña y quitá esas variables del entorno.", admin.getEmail());
    }
}
