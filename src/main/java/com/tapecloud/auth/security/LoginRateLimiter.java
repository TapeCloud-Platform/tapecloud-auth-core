package com.tapecloud.auth.security;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Limita intentos de login por identificador (cuenta) y por IP de origen, en paralelo,
 * para frenar tanto credential stuffing contra una cuenta como fuerza bruta desde un origen.
 */
@Component
public class LoginRateLimiter {

    private final AttemptTracker byIdentifier = new AttemptTracker(
            5, Duration.ofMinutes(15), Duration.ofSeconds(30), Duration.ofMinutes(15));
    private final AttemptTracker byIp = new AttemptTracker(
            20, Duration.ofMinutes(15), Duration.ofSeconds(30), Duration.ofMinutes(15));

    public void checkAllowed(String identifier, String clientIp) {
        byIdentifier.checkAllowed("id:" + identifier);
        byIp.checkAllowed("ip:" + clientIp);
    }

    public void recordFailure(String identifier, String clientIp) {
        byIdentifier.recordFailure("id:" + identifier);
        byIp.recordFailure("ip:" + clientIp);
    }

    public void recordSuccess(String identifier, String clientIp) {
        byIdentifier.recordSuccess("id:" + identifier);
        byIp.recordSuccess("ip:" + clientIp);
    }
}
