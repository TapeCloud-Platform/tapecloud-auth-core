package com.tapecloud.auth.service;

import com.tapecloud.auth.config.JwtService;
import com.tapecloud.auth.exception.TotpRequiredException;
import com.tapecloud.auth.security.LoginRateLimiter;
import com.tapecloud.auth.user.dto.AuthResponse;
import com.tapecloud.auth.user.dto.ChangePasswordRequest;
import com.tapecloud.auth.user.dto.LoginRequest;
import com.tapecloud.auth.user.dto.RegisterRequest;
import com.tapecloud.auth.user.dto.RegisterResponse;
import com.tapecloud.auth.user.dto.ResendCodeRequest;
import com.tapecloud.auth.user.dto.TotpDisableRequest;
import com.tapecloud.auth.user.dto.TotpEnableRequest;
import com.tapecloud.auth.user.dto.TotpSetupResponse;
import com.tapecloud.auth.user.dto.UpdateAvatarRequest;
import com.tapecloud.auth.user.dto.UpdateUsernameRequest;
import com.tapecloud.auth.user.dto.VerifyEmailRequest;
import com.tapecloud.auth.user.entity.AppUser;
import com.tapecloud.auth.user.entity.Role;
import com.tapecloud.auth.user.repository.AppUserRepository;
import com.tapecloud.auth.user.repository.RoleRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long VERIFICATION_CODE_TTL_MINUTES = 5;

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TotpService totpService;
    private final LoginRateLimiter loginRateLimiter;

    @Value("${tapecloud.admin.emails:totosanchez2610@gmail.com,admin@tapecloud.com}")
    private String adminEmailsProperty;

    public AuthService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            TotpService totpService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.totpService = totpService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim();
        String normalizedUsername = request.username().trim();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso");
        }

        Role defaultRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        AppUser user = new AppUser(
                normalizedEmail.toLowerCase(Locale.ROOT),
                passwordEncoder.encode(request.password()),
                normalizedUsername
        );
        user.addRole(defaultRole);

        if (isAdminEmail(normalizedEmail)) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            user.addRole(adminRole);
        }

        assignVerificationCode(user);
        userRepository.save(user);
        emailService.sendVerificationCode(user.getEmail(), user.getVerificationCode());

        return new RegisterResponse(user.getEmail(), "Te enviamos un código de verificación a tu email");
    }

    private void assignVerificationCode(AppUser user) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(Instant.now().plus(VERIFICATION_CODE_TTL_MINUTES, ChronoUnit.MINUTES));
        user.setEmailVerified(false);
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("El email ya fue verificado");
        }
        if (user.getVerificationCode() == null || user.getVerificationCodeExpiresAt() == null
                || Instant.now().isAfter(user.getVerificationCodeExpiresAt())) {
            throw new IllegalArgumentException("El código venció, pedí uno nuevo");
        }
        if (!user.getVerificationCode().equals(request.code().trim())) {
            throw new IllegalArgumentException("El código es incorrecto");
        }

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        return buildResponse(user);
    }

    @Transactional
    public void resendVerificationCode(ResendCodeRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("El email ya fue verificado");
        }

        assignVerificationCode(user);
        userRepository.save(user);
        emailService.sendVerificationCode(user.getEmail(), user.getVerificationCode());
    }

    private boolean isAdminEmail(String email) {
        if (adminEmailsProperty == null || adminEmailsProperty.isBlank()) {
            return false;
        }
        String lowerEmail = email.toLowerCase(Locale.ROOT);
        return Arrays.stream(adminEmailsProperty.split(","))
                .map(String::trim)
                .map(e -> e.toLowerCase(Locale.ROOT))
                .anyMatch(lowerEmail::equals);
    }


    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String clientIp) {
        String identifier = request.identifier().trim();
        loginRateLimiter.checkAllowed(identifier, clientIp);

        AppUser user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseGet(() -> {
                    loginRateLimiter.recordFailure(identifier, clientIp);
                    throw new IllegalArgumentException("Credenciales inválidas");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginRateLimiter.recordFailure(identifier, clientIp);
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        if (!user.isEnabled()) {
            loginRateLimiter.recordFailure(identifier, clientIp);
            throw new IllegalArgumentException("Esta cuenta está deshabilitada");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("Verificá tu email antes de iniciar sesión");
        }

        if (user.isTotpEnabled()) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                // Todavía no cuenta como intento fallido: es el primer paso normal del flujo de 2FA.
                throw new TotpRequiredException("Ingresá el código de tu app de autenticación");
            }
            if (!totpService.verifyCode(user.getTotpSecret(), request.totpCode().trim())) {
                loginRateLimiter.recordFailure(identifier, clientIp);
                throw new TotpRequiredException("El código de autenticación es incorrecto");
            }
        }

        loginRateLimiter.recordSuccess(identifier, clientIp);
        return buildResponse(user);
    }

    @Transactional
    public TotpSetupResponse setupTotp(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.isTotpEnabled()) {
            throw new IllegalArgumentException("La verificación en dos pasos ya está activada, desactivala primero para generar un nuevo código QR");
        }

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        return new TotpSetupResponse(secret, totpService.generateQrCodeDataUri(user.getEmail(), secret));
    }

    @Transactional
    public void enableTotp(String email, TotpEnableRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.isTotpEnabled()) {
            throw new IllegalArgumentException("La verificación en dos pasos ya está activada");
        }
        if (user.getTotpSecret() == null) {
            throw new IllegalArgumentException("Primero generá un código QR");
        }
        if (!totpService.verifyCode(user.getTotpSecret(), request.code().trim())) {
            throw new IllegalArgumentException("El código es incorrecto");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void disableTotp(String email, TotpDisableRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña es incorrecta");
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public boolean isTotpEnabled(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(AppUser::isTotpEnabled)
                .orElse(false);
    }

    private AuthResponse buildResponse(AppUser user) {
        String token = jwtService.generateToken(user);
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return new AuthResponse(token, user.getEmail(), user.getUsername(), user.getDisplayName(), roles, user.getAvatarDataUri());
    }

    @Transactional
    public AuthResponse updateUsername(String email, UpdateUsernameRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String normalizedUsername = request.username().trim();
        if (!normalizedUsername.equalsIgnoreCase(user.getUsername())
                && userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso");
        }

        // El nombre de usuario es tanto el identificador de login como el nombre público,
        // así que se mantienen sincronizados (igual que al registrarse).
        user.setUsername(normalizedUsername);
        user.setDisplayName(normalizedUsername);
        userRepository.save(user);
        return buildResponse(user);
    }

    @Transactional
    public AuthResponse updateAvatar(String email, UpdateAvatarRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String avatarDataUri = request.avatarDataUri();
        if (avatarDataUri != null && !avatarDataUri.isBlank() && !avatarDataUri.startsWith("data:image/")) {
            throw new IllegalArgumentException("La imagen no es válida");
        }

        user.setAvatarDataUri(avatarDataUri == null || avatarDataUri.isBlank() ? null : avatarDataUri);
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
        user.bumpTokenVersion();
        userRepository.save(user);
    }

    public List<String> currentUserRoles(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Transactional(readOnly = true)
    public String getAvatarDataUri(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(AppUser::getAvatarDataUri)
                .orElse(null);
    }
}
