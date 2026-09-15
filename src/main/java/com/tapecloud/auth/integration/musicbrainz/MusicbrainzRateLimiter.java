package com.tapecloud.auth.integration.musicbrainz;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.web.client.RestClientException;

/**
 * MusicBrainz pide no superar 1 petición por segundo, así que las llamadas se serializan
 * y se espacian antes de salir.
 */
class MusicbrainzRateLimiter {

    private static final long MIN_INTERVAL_MS = 1100;

    // El servidor público devuelve 503 de forma intermitente aun respetando el ritmo.
    private static final int MAX_ATTEMPTS = 3;

    private final ReentrantLock lock = new ReentrantLock(true);
    private long lastCallAt = 0;

    <T> T call(java.util.function.Supplier<T> action) {
        RestClientException lastError = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return callOnce(action);
            } catch (RestClientException e) {
                lastError = e;
            }
        }

        throw lastError;
    }

    private <T> T callOnce(java.util.function.Supplier<T> action) {
        lock.lock();
        try {
            long waitMs = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastCallAt);
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta a MusicBrainz interrumpida", e);
        } finally {
            lastCallAt = System.currentTimeMillis();
            lock.unlock();
        }
    }
}
