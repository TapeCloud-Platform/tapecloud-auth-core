package com.tapecloud.auth.security;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Frena fuerza bruta sobre los códigos de 6 dígitos (verify-email) y el
 * bombardeo de emails (resend-code), por cuenta y por IP en paralelo.
 */
@Component
public class EmailCodeRateLimiter {

    private final AttemptTracker byEmail = new AttemptTracker(
            5, Duration.ofMinutes(15), Duration.ofSeconds(30), Duration.ofMinutes(15));
    private final AttemptTracker byIp = new AttemptTracker(
            20, Duration.ofMinutes(15), Duration.ofSeconds(30), Duration.ofMinutes(15));

    public void checkAllowed(String email, String clientIp) {
        byEmail.checkAllowed("email:" + email);
        byIp.checkAllowed("ip:" + clientIp);
    }

    public void recordFailure(String email, String clientIp) {
        byEmail.recordFailure("email:" + email);
        byIp.recordFailure("ip:" + clientIp);
    }

    public void recordSuccess(String email, String clientIp) {
        byEmail.recordSuccess("email:" + email);
        byIp.recordSuccess("ip:" + clientIp);
    }

    /** Cuenta un intento aunque haya salido bien (para resend: cada envío suma). */
    public void recordAttempt(String email, String clientIp) {
        byEmail.recordFailure("email:" + email);
        byIp.recordFailure("ip:" + clientIp);
    }
}
