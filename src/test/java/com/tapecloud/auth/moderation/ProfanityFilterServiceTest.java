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

    @Test
    void commonSpanishWordsAreAllowed() throws Exception {
        // Hermético (no depende de los .txt): bloquea "con"/"del" por
        // extraWords pero la lista de permitidas gana, como en producción.
        ProfanityFilterService service = serviceWithAllow(
                new String[] {"con", "del", "pelotudo"},
                new String[] {"con", "del", "la", "una", "ano", "tras", "vale", "esta",
                        "pero", "final", "poco", "bajo", "que", "se", "no", "en", "el",
                        "su", "las", "un", "de"});
        // Español común que choca con listas de otros idiomas ("con" en
        // francés, "ano" de "año") no debe bloquear reseñas normales.
        assertThat(service.containsProfanity("La mejor película del año, con una fotografía increíble"))
                .isFalse();
        assertThat(service.containsProfanity("Con este álbum confirmaron su lugar en la historia"))
                .isFalse();
        service.requireCleanUsername("con");
    }

    @Test
    void realInsultsStillBlocked() {
        ProfanityFilterService service = serviceWithWords("puta", "pelotudo");
        assertThat(service.containsProfanity("esta puta madre")).isTrue();
        assertThatThrownBy(() -> service.requireCleanUsername("puta.madre"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ProfanityFilterService serviceWithAllow(String[] blockWords, String[] allowWords)
            throws Exception {
        ProfanityProperties props = new ProfanityProperties();
        props.setEnabled(true);
        props.setExtraWords(String.join(",", blockWords));
        ProfanityFilterService service = new ProfanityFilterService(props);
        service.load();
        java.lang.reflect.Field allow =
                ProfanityFilterService.class.getDeclaredField("allow");
        allow.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<String> set = (java.util.Set<String>) allow.get(service);
        for (String word : allowWords) {
            set.add(ProfanityFilterService.normalize(word));
        }
        return service;
    }
}
