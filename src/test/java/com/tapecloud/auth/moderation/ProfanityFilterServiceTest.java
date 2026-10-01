package com.tapecloud.auth.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProfanityFilterServiceTest {

    private ProfanityFilterService serviceWithWords(String... words) {
        ProfanityProperties props = new ProfanityProperties();
        props.setEnabled(true);
        props.setExtraWords(String.join(",", words));
        ProfanityFilterService service = new ProfanityFilterService(props);
        service.load();
        return service;
    }

    @Test
    void cleanTextPasses() {
        ProfanityFilterService service = serviceWithWords("pelotudo");
        assertThat(service.containsProfanity("Una película excelente")).isFalse();
        service.requireClean("Una película excelente", "La reseña");
    }

    @Test
    void blocksWithWordBoundaries() {
        ProfanityFilterService service = serviceWithWords("puta");
        assertThat(service.containsProfanity("esta puta madre")).isTrue();
        // Scunthorpe: no bloquear dentro de otra palabra
        assertThat(service.containsProfanity("computadora")).isFalse();
    }

    @Test
    void normalizesLeetAndDiacritics() {
        ProfanityFilterService service = serviceWithWords("puta");
        assertThat(service.containsProfanity("pút4")).isTrue();
        assertThat(service.containsProfanity("putaaa")).isTrue();
    }

    @Test
    void usernameTokens() {
        ProfanityFilterService service = serviceWithWords("puta", "fucker");
        service.requireCleanUsername("juan123");
        assertThatThrownBy(() -> service.requireCleanUsername("puta.madre"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.requireCleanUsername("fucker99"))
                .isInstanceOf(IllegalArgumentException.class);
        // "computadora99" no debe bloquearse aunque termine en dígitos
        service.requireCleanUsername("computadora99");
    }

    @Test
    void disabledFilterAllowsEverything() {
        ProfanityProperties props = new ProfanityProperties();
        props.setEnabled(false);
        props.setExtraWords("puta");
        ProfanityFilterService service = new ProfanityFilterService(props);
        service.load();
        assertThat(service.containsProfanity("puta")).isFalse();
    }
}
