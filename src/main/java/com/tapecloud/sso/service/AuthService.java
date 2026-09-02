package com.tapecloud.sso.service;

import com.tapecloud.sso.user.dto.AuthRequest;
import com.tapecloud.sso.user.dto.AuthResponse;
import com.tapecloud.sso.user.entity.AppUser;
import com.tapecloud.sso.user.entity.Role;
import com.tapecloud.sso.user.repository.AppUserRepository;
import com.tapecloud.sso.user.repository.RoleRepository;
import com.tapecloud.sso.config.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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

    public AuthResponse buildResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        return new AuthResponse(token, user.getEmail(), roles);
    }

    public List<String> currentUserRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
