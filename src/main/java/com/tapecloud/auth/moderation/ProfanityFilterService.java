package com.tapecloud.auth.moderation;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    // Aho-Corasick sobre las entradas colapsadas: un solo pase lineal por
    // texto en vez de un regex por palabra (con 200k entradas, el loop de
    // Pattern por request es inviable). La semantica es la misma que antes:
    // coincidencia con limites de palabra [\p{L}\p{N}_] a ambos lados.
    private final List<String> entries = new ArrayList<>();
    private int[] fail = new int[0];
    private int[] childHead = new int[0];
    private char[] edgeChar = new char[0];
    private int[] edgeTo = new int[0];
    private int[] edgeNext = new int[0];
    private int[] outHead = new int[0];
    private int[] outNext = new int[0];
    private boolean[] hasOut = new boolean[0];
    private int nodeCount;
    private int edgeCount;
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

        Set<String> entrySet = new LinkedHashSet<>();
        for (String word : words) {
            // Se normaliza igual que el texto chequeado (leet incluido): las
            // variantes que colapsan a la misma base son redundantes.
            String collapsed = normalize(word);
            if (collapsed.isEmpty() || !hasAlnum(collapsed)) {
                continue;
            }
            if (FALSE_POSITIVES.contains(collapsed)) {
                continue;
            }
            if (entrySet.add(collapsed)) {
                entries.add(collapsed);
            }
        }
        buildAutomaton();
        wordCount = entries.size();
        log.info("Filtro de lenguaje cargado con {} palabras (enabled={})", wordCount, properties.isEnabled());
    }

    private static final Set<String> FALSE_POSITIVES = Set.of(
            "john", "member", "members", "nuts", "bear", "bears", "rack", "rod");

    private static boolean hasAlnum(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                return true;
            }
        }
        return false;
    }

    /** Construye el trie Aho-Corasick con arreglos compactos (sin HashMap por nodo). */
    private void buildAutomaton() {
        int totalChars = 0;
        for (String word : entries) {
            totalChars += word.length();
        }
        int maxNodes = 1 + totalChars;
        fail = new int[maxNodes];
        childHead = new int[maxNodes];
        outHead = new int[maxNodes];
        hasOut = new boolean[maxNodes];
        Arrays.fill(childHead, -1);
        Arrays.fill(outHead, -1);
        edgeChar = new char[totalChars + 1];
        edgeTo = new int[totalChars + 1];
        edgeNext = new int[totalChars + 1];
        outNext = new int[Math.max(1, entries.size())];
        nodeCount = 1;
        edgeCount = 0;

        for (int id = 0; id < entries.size(); id++) {
            String word = entries.get(id);
            int node = 0;
            for (int i = 0; i < word.length(); i++) {
                node = insertEdge(node, word.charAt(i));
            }
            outNext[id] = outHead[node];
            outHead[node] = id;
            hasOut[node] = true;
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int e = childHead[0]; e != -1; e = edgeNext[e]) {
            int child = edgeTo[e];
            fail[child] = 0;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            int node = queue.removeFirst();
            if (hasOut[fail[node]]) {
                hasOut[node] = true;
            }
            for (int e = childHead[node]; e != -1; e = edgeNext[e]) {
                int child = edgeTo[e];
                int fallback = fail[node];
                int next;
                while (fallback != 0 && (next = findEdge(fallback, edgeChar[e])) == -1) {
                    fallback = fail[fallback];
                }
                next = findEdge(fallback, edgeChar[e]);
                fail[child] = (next == -1) ? 0 : next;
                queue.add(child);
            }
        }

        fail = Arrays.copyOf(fail, nodeCount);
        childHead = Arrays.copyOf(childHead, nodeCount);
        outHead = Arrays.copyOf(outHead, nodeCount);
        hasOut = Arrays.copyOf(hasOut, nodeCount);
        edgeChar = Arrays.copyOf(edgeChar, edgeCount);
        edgeTo = Arrays.copyOf(edgeTo, edgeCount);
        edgeNext = Arrays.copyOf(edgeNext, edgeCount);
    }

    private int insertEdge(int node, char c) {
        for (int e = childHead[node]; e != -1; e = edgeNext[e]) {
            if (edgeChar[e] == c) {
                return edgeTo[e];
            }
        }
        int child = nodeCount++;
        edgeChar[edgeCount] = c;
        edgeTo[edgeCount] = child;
        edgeNext[edgeCount] = childHead[node];
        childHead[node] = edgeCount;
        edgeCount++;
        return child;
    }

    private int findEdge(int node, char c) {
        for (int e = childHead[node]; e != -1; e = edgeNext[e]) {
            if (edgeChar[e] == c) {
                return edgeTo[e];
            }
        }
        return -1;
    }

    /** Equivale a [\p{L}\p{N}_] con UNICODE_CHARACTER_CLASS. */
    private static boolean isWordChar(char c) {
        if (c == '_') {
            return true;
        }
        int type = Character.getType(c);
        return type == Character.UPPERCASE_LETTER
                || type == Character.LOWERCASE_LETTER
                || type == Character.TITLECASE_LETTER
                || type == Character.MODIFIER_LETTER
                || type == Character.OTHER_LETTER
                || type == Character.DECIMAL_DIGIT_NUMBER
                || type == Character.LETTER_NUMBER
                || type == Character.OTHER_NUMBER;
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
        int node = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int next;
            while (node != 0 && (next = findEdge(node, c)) == -1) {
                node = fail[node];
            }
            next = findEdge(node, c);
            node = (next == -1) ? 0 : next;
            if (!hasOut[node]) {
                continue;
            }
            for (int t = node; t != 0; t = fail[t]) {
                for (int id = outHead[t]; id != -1; id = outNext[id]) {
                    int len = entries.get(id).length();
                    int start = i - len + 1;
                    if (start < 0) {
                        continue;
                    }
                    if ((start == 0 || !isWordChar(normalized.charAt(start - 1)))
                            && (i + 1 == normalized.length() || !isWordChar(normalized.charAt(i + 1)))) {
                        return normalized.substring(start, i + 1);
                    }
                }
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
        for (String token : candidates) {
            if (!token.isEmpty() && matchesWhole(token)) {
                throw new IllegalArgumentException(
                        "Ese nombre de usuario contiene lenguaje no permitido. Elegí otro.");
            }
        }
    }

    /** Equivale al viejo pattern.matcher(token).matches(): la entrada cubre todo el token. */
    private boolean matchesWhole(String token) {
        int node = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            int next;
            while (node != 0 && (next = findEdge(node, c)) == -1) {
                node = fail[node];
            }
            next = findEdge(node, c);
            node = (next == -1) ? 0 : next;
            if (!hasOut[node]) {
                continue;
            }
            for (int t = node; t != 0; t = fail[t]) {
                for (int id = outHead[t]; id != -1; id = outNext[id]) {
                    int len = entries.get(id).length();
                    int start = i - len + 1;
                    if (start == 0 && i == token.length() - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getWordCount() {
        return wordCount;
    }
}
