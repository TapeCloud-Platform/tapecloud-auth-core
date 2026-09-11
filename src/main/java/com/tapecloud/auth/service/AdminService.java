package com.tapecloud.auth.service;

import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import com.tapecloud.auth.user.repository.RoleRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminService(AppUserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "displayName", user.getDisplayName(),
                        "roles", user.getRoles().stream().map(Role::getName).toList(),
                        "enabled", user.isEnabled()
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> grantAdminRole(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

        user.addRole(adminRole);
        userRepository.save(user);

        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "roles", user.getRoles().stream().map(Role::getName).toList()
        );
    }

    @Transactional
    public Map<String, Object> revokeAdminRole(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.getRoles().removeIf(role -> "ROLE_ADMIN".equals(role.getName()));
        userRepository.save(user);

        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "roles", user.getRoles().stream().map(Role::getName).toList()
        );
    }
}
