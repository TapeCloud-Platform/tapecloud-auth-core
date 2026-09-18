package com.tapecloud.auth.api;

import com.tapecloud.auth.service.AuthService;
import com.tapecloud.auth.user.dto.AuthResponse;
import com.tapecloud.auth.user.dto.ChangePasswordRequest;
import com.tapecloud.auth.user.dto.LoginRequest;
import com.tapecloud.auth.user.dto.RegisterRequest;
import com.tapecloud.auth.user.dto.RegisterResponse;
import com.tapecloud.auth.user.dto.ResendCodeRequest;
import com.tapecloud.auth.user.dto.UpdateUsernameRequest;
import com.tapecloud.auth.user.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authService.currentUserRoles(authentication)
        ));
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
