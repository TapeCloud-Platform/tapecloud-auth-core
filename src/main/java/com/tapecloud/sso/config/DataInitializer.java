package com.tapecloud.sso.config;

import com.tapecloud.sso.user.entity.Role;
import com.tapecloud.sso.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfAbsent("ROLE_USER");
        createRoleIfAbsent("ROLE_ADMIN");
    }

    private void createRoleIfAbsent(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name));
        }
    }
}
