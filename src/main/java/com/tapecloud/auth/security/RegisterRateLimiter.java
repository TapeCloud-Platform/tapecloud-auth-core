package com.tapecloud.auth.security;

import java.time.Duration;
import org.springframework.stereotype.Component;

/** Limita registros por IP de origen para frenar creación masiva/automatizada de cuentas. */
@Component
public class RegisterRateLimiter {

    private final AttemptTracker byIp = new AttemptTracker(
            10, Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofMinutes(30));

    public void checkAllowed(String clientIp) {
        byIp.checkAllowed("ip:" + clientIp);
    }

    public void recordAttempt(String clientIp) {
        byIp.recordFailure("ip:" + clientIp);
    }
}
