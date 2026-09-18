package com.tapecloud.auth.security;

import com.tapecloud.auth.exception.TooManyAttemptsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de intentos fallidos en memoria, con backoff exponencial acotado por clave
 * (nunca bloqueo permanente: evita que un tercero deje una cuenta legítima inaccesible).
 * No es apto para despliegues multi-instancia sin estado compartido (ej. Redis).
 */
public class AttemptTracker {

    private record Attempt(int count, Instant windowStart, Instant lockedUntil) {
    }

    private final int maxAttemptsBeforeLockout;
    private final Duration window;
    private final Duration baseLockout;
    private final Duration maxLockout;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public AttemptTracker(int maxAttemptsBeforeLockout, Duration window, Duration baseLockout, Duration maxLockout) {
        this.maxAttemptsBeforeLockout = maxAttemptsBeforeLockout;
        this.window = window;
        this.baseLockout = baseLockout;
        this.maxLockout = maxLockout;
    }

    public void checkAllowed(String key) {
        Attempt current = attempts.get(normalize(key));
        if (current != null && current.lockedUntil() != null && Instant.now().isBefore(current.lockedUntil())) {
            long secondsLeft = Duration.between(Instant.now(), current.lockedUntil()).toSeconds() + 1;
            throw new TooManyAttemptsException("Demasiados intentos. Probá de nuevo en " + secondsLeft + " segundos.");
        }
    }

    public void recordFailure(String key) {
        attempts.compute(normalize(key), (unused, current) -> {
            Instant now = Instant.now();
            if (current == null || Duration.between(current.windowStart(), now).compareTo(window) > 0) {
                return new Attempt(1, now, null);
            }

            int count = current.count() + 1;
            Instant lockedUntil = null;
            if (count >= maxAttemptsBeforeLockout) {
                int extraFailures = count - maxAttemptsBeforeLockout;
                long backoffSeconds = baseLockout.toSeconds() * (1L << Math.min(extraFailures, 5));
                backoffSeconds = Math.min(backoffSeconds, maxLockout.toSeconds());
                lockedUntil = now.plusSeconds(backoffSeconds);
            }
            return new Attempt(count, current.windowStart(), lockedUntil);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(normalize(key));
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
