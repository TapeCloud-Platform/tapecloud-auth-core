package com.tapecloud.auth.api;

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
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
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

    public AuthController(AuthService authService, RegisterRateLimiter registerRateLimiter) {
        this.authService = authService;
        this.registerRateLimiter = registerRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = httpRequest.getRemoteAddr();
        registerRateLimiter.checkAllowed(clientIp);
        registerRateLimiter.recordAttempt(clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        authService.resendVerificationCode(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", authentication.getName());
        body.put("roles", authService.currentUserRoles(authentication));
        body.put("totpEnabled", authService.isTotpEnabled(authentication.getName()));
        body.put("avatarDataUri", authService.getAvatarDataUri(authentication.getName()));
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/me/avatar")
    public ResponseEntity<AuthResponse> updateAvatar(
            Authentication authentication,
            @Valid @RequestBody UpdateAvatarRequest request
    ) {
        return ResponseEntity.ok(authService.updateAvatar(authentication.getName(), request));
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
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return ResponseEntity.ok(authService.updateUsername(authentication.getName(), request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
