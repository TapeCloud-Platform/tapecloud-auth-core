package com.tapecloud.auth.service;

import com.tapecloud.auth.config.JwtService;
import com.tapecloud.auth.user.dto.AuthRequest;
import com.tapecloud.auth.user.dto.AuthResponse;
import com.tapecloud.auth.user.dto.ChangePasswordRequest;
import com.tapecloud.auth.user.dto.UpdateDisplayNameRequest;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import com.tapecloud.auth.user.repository.RoleRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String normalizedEmail = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        AppUser user = new AppUser(normalizedEmail.toLowerCase(Locale.ROOT), passwordEncoder.encode(request.password()));
        user.addRole(defaultRole);
        userRepository.save(user);

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return new AuthResponse(token, user.getEmail(), user.getDisplayName(), roles);
    }

    @Transactional
    public AuthResponse updateDisplayName(String email, UpdateDisplayNameRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setDisplayName(request.displayName().trim());
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public List<String> currentUserRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }
}
