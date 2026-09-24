package com.tapecloud.auth.api;

import com.tapecloud.auth.security.EmailCodeRateLimiter;
import com.tapecloud.auth.security.RegisterRateLimiter;
import com.tapecloud.auth.service.AuthService;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegisterRateLimiter registerRateLimiter;
    private final EmailCodeRateLimiter emailCodeRateLimiter;

    public AuthController(AuthService authService, RegisterRateLimiter registerRateLimiter,
            EmailCodeRateLimiter emailCodeRateLimiter) {
        this.authService = authService;
        this.registerRateLimiter = registerRateLimiter;
        this.emailCodeRateLimiter = emailCodeRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIp(httpRequest);
        registerRateLimiter.checkAllowed(clientIp);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
        } catch (IllegalArgumentException ex) {
            // Solo los intentos fallidos consumen cuota; los registros exitosos no penalizan la IP.
            registerRateLimiter.recordAttempt(clientIp);
            throw ex;
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = clientIp(httpRequest);
        String emailKey = request.email().trim().toLowerCase(Locale.ROOT);
        emailCodeRateLimiter.checkAllowed(emailKey, clientIp);
        try {
            AuthResponse response = authService.verifyEmail(request);
            emailCodeRateLimiter.recordSuccess(emailKey, clientIp);
            addTokenCookie(httpResponse, response.token());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            emailCodeRateLimiter.recordFailure(emailKey, clientIp);
            throw ex;
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(
            @Valid @RequestBody ResendCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = clientIp(httpRequest);
        String emailKey = request.email().trim().toLowerCase(Locale.ROOT);
        emailCodeRateLimiter.checkAllowed(emailKey, clientIp);
        // Cada reenvío consume cuota, salga bien o no: frena el bombardeo de emails.
        emailCodeRateLimiter.recordAttempt(emailKey, clientIp);
        authService.resendVerificationCode(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request, clientIp(httpRequest));
        addTokenCookie(httpResponse, response.token());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", authentication.getName());
        body.put("roles", authService.currentUserRoles(authentication));
        body.put("totpEnabled", authService.isTotpEnabled(authentication.getName()));
        body.put("avatarDataUri", authService.getAvatarDataUri(authentication.getName()));
        body.put("displayName", authService.getDisplayName(authentication.getName()));
        body.put("username", authService.getUsername(authentication.getName()));
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<AuthResponse> updateAvatar(
            Authentication authentication,
            @Valid @RequestBody UpdateAvatarRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.updateAvatar(authentication.getName(), request);
        addTokenCookie(httpResponse, response.token());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupResponse> setupTotp(Authentication authentication) {
        return ResponseEntity.ok(authService.setupTotp(authentication.getName()));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Void> enableTotp(
            Authentication authentication,
            @Valid @RequestBody TotpEnableRequest request
    ) {
        authService.enableTotp(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Void> disableTotp(
            Authentication authentication,
            @Valid @RequestBody TotpDisableRequest request
    ) {
        authService.disableTotp(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/username")
    public ResponseEntity<AuthResponse> updateUsername(
            Authentication authentication,
            @Valid @RequestBody UpdateUsernameRequest request,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.updateUsername(authentication.getName(), request);
        addTokenCookie(httpResponse, response.token());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse httpResponse
    ) {
        authService.changePassword(authentication.getName(), request);
        clearTokenCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletResponse httpResponse) {
        if (authentication != null) {
            authService.logout(authentication.getName());
        }
        clearTokenCookie(httpResponse);
        return ResponseEntity.noContent().build();
    }

    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("tapecloud_token", token)
                .httpOnly(true)
                .secure(false) // localhost es http; en prod detrás de https poner true vía prop si hace falta
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(8))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("tapecloud_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // IP real del cliente cuando hay un proxy delante (docker/nginx). Solo se usa
    // como clave de rate-limit, no para decisiones de autorización.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
