package com.tapecloud.auth.moderation;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Filtro de malas palabras en varios idiomas.
 *
 * <p>La lista NO está hardcodeada: se carga de {@code classpath:bad-words-*.txt}
 * ({@code bad-words-base.txt} + los archivos por idioma + {@code bad-words-custom.txt},
 * donde el admin agrega términos sin tocar código) más {@code profanity.extra-words}.
 * A diferencia del censurado con asteriscos, este filtro BLOQUEA con un aviso
 * (400) para que el usuario corrija su texto.
 *
 * <p>La comparación normaliza tildes, mayúsculas y leet-speak básico, y exige
 * límites de palabra para no bloquear falsos positivos (ej. "computadora" no
 * dispara "puta").
 */
@Configuration
@EnableConfigurationProperties(ProfanityProperties.class)
public class ProfanityFilterService {

    private static final Logger log = LoggerFactory.getLogger(ProfanityFilterService.class);

    private final ProfanityProperties properties;
    private final List<Pattern> patterns = new ArrayList<>();
    private int wordCount = 0;

    public ProfanityFilterService(ProfanityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        Set<String> words = new LinkedHashSet<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:bad-words-*.txt");
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream();
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String word = normalizeWord(line);
                        if (!word.isEmpty()) {
                            words.add(word);
                        }
                    }
                } catch (Exception e) {
                    log.warn("No se pudo leer {}: {}", resource.getFilename(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("No se encontraron listas en classpath:bad-words-*.txt");
        }

        if (properties.getExtraWords() != null && !properties.getExtraWords().isBlank()) {
            Arrays.stream(properties.getExtraWords().split(","))
                    .map(ProfanityFilterService::normalizeWord)
                    .filter(w -> !w.isEmpty())
                    .forEach(words::add);
        }

        for (String word : words) {
            // Límites de palabra a ambos lados (unicode) para evitar falsos positivos.
            patterns.add(Pattern.compile(
                    "(?<![\\p{L}\\p{N}_])" + Pattern.quote(word) + "(?![\\p{L}\\p{N}_])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));
        }
        wordCount = words.size();
        log.info("Filtro de lenguaje cargado con {} palabras (enabled={})", wordCount, properties.isEnabled());
    }

    /** Normaliza una línea de los archivos: minúsculas, sin tildes, sin espacios. */
    private static String normalizeWord(String line) {
        String trimmed = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return "";
        }
        return stripDiacritics(trimmed);
    }

    /** Normaliza un texto libre para comparar: minúsculas, sin tildes, leet básico. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = stripDiacritics(text.toLowerCase(Locale.ROOT));
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            out.append(switch (c) {
                case '4', '@' -> 'a';
                case '3' -> 'e';
                case '1', '!' -> 'i';
                case '0' -> 'o';
                case '5', '$' -> 's';
                case '7' -> 't';
                case '+' -> 't';
                default -> c;
            });
        }
        // Colapsa repeticiones de 3+ ("putaaa" -> "puta").
        return out.toString().replaceAll("(.)\\1{2,}", "$1");
    }

    private static String stripDiacritics(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Devuelve la palabra bloqueada encontrada o {@code null} si el texto está limpio. */
    public String findBlockedWord(String text) {
        if (!properties.isEnabled() || text == null || text.isBlank()) {
            return null;
        }
        String normalized = normalize(text);
        for (Pattern pattern : patterns) {
            var matcher = pattern.matcher(normalized);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    public boolean containsProfanity(String text) {
        return findBlockedWord(text) != null;
    }

    /**
     * Valida un campo de texto libre (reseña, comentario). Lanza
     * {@link IllegalArgumentException} (400) con aviso si hay coincidencia.
     */
    public void requireClean(String text, String fieldLabel) {
        String blocked = findBlockedWord(text);
        if (blocked != null) {
            throw new IllegalArgumentException(
                    fieldLabel + " contiene lenguaje no permitido. Revisá tu texto y probá de nuevo.");
        }
    }

    /**
     * Valida un nombre de usuario (un solo token sin espacios): lo parte por
     * separadores y bloquea si algún token coincide con la lista. Así
     * "puta.madre" se bloquea pero "computadora" no (Scunthorpe). También se
     * ignoran dígitos en los bordes ("fucker99" sí se bloquea).
     */
    public void requireCleanUsername(String username) {
        if (!properties.isEnabled() || username == null || username.isBlank()) {
            return;
        }
        String normalized = normalize(username);
        Set<String> tokenSet = new LinkedHashSet<>(Arrays.asList(normalized.split("[^a-z0-9]+")));
        tokenSet.add(normalized.replaceAll("[^a-z0-9]", ""));
        Set<String> candidates = new LinkedHashSet<>(tokenSet);
        for (String token : tokenSet) {
            candidates.add(token.replaceAll("^[0-9]+|[0-9]+$", ""));
        }
        for (Pattern pattern : patterns) {
            for (String token : candidates) {
                if (!token.isEmpty() && pattern.matcher(token).matches()) {
                    throw new IllegalArgumentException(
                            "Ese nombre de usuario contiene lenguaje no permitido. Elegí otro.");
                }
            }
        }
    }

    public int getWordCount() {
        return wordCount;
    }
}
