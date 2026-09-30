package com.tapecloud.auth.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del filtro de lenguaje. La lista de palabras NO vive en el
 * código: se carga de {@code classpath:bad-words-*.txt} (ver
 * {@code bad-words-base.txt} y {@code bad-words-custom.txt}, este último
 * editable por el admin sin tocar código) más las palabras extra que se
 * definan por variable de entorno.
 */
@ConfigurationProperties(prefix = "profanity")
public class ProfanityProperties {

    /** Activa/desactiva el filtro (PROFANITY_ENABLED). */
    private boolean enabled = true;

    /**
     * Palabras extra separadas por coma (PROFANITY_EXTRA_WORDS).
     * Sirve para agregar términos sin tocar los archivos de recursos.
     */
    private String extraWords = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExtraWords() {
        return extraWords;
    }

    public void setExtraWords(String extraWords) {
        this.extraWords = extraWords;
    }
}
