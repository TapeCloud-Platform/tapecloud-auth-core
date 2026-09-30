package com.tapecloud.auth.integration.lastfm;

import com.tapecloud.auth.integration.lastfm.dto.LastfmTopAlbumsResponse.AlbumEntry;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Last.fm lista cada edición como un álbum distinto ("Signos", "Signos (Remastered)",
 * "Comfort y Música" vs "Comfort Y Musica"), así que hay que normalizar títulos para
 * poder descartar repetidos.
 */
final class AlbumTitleFilter {

    // Sufijos entre paréntesis o tras guion que solo indican una reedición del mismo disco.
    private static final Pattern BRACKETED = Pattern.compile("\\s*[\\(\\[][^)\\]]*[\\)\\]]");
    private static final Pattern EDITION_SUFFIX = Pattern.compile(
            "\\s*[-–]\\s*(remaster|remastered|remasterizado|deluxe|edition|edici|version|versi|"
                    + "single|ep|live|mix|bonus|expanded|anniversary|reissue).*",
            Pattern.CASE_INSENSITIVE);

    // Numeración de soportes físicos: "Chau Soda CD 1", "20 Grandes Exitos, Disc 1".
    private static final Pattern DISC_SUFFIX = Pattern.compile(
            "\\s*,?\\s*(cd|disc|disco|disk)\\s*\\d+\\s*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    // "El Último Concierto, Pt. B" y "El Último Concierto B" son el mismo disco.
    private static final Pattern PART_WORD = Pattern.compile("\\b(pt|part|parte|vol|volume|volumen)\\b");

    private static final Pattern NON_ALBUM = Pattern.compile(
            "\\b(single|remix|instrumental|karaoke|demo)\\b", Pattern.CASE_INSENSITIVE);

    private AlbumTitleFilter() {
    }

    /** Título sin el sufijo de edición, conservando el formato original para mostrarlo. */
    static String display(String title) {
        if (title == null) {
            return "";
        }
        String cleaned = BRACKETED.matcher(title).replaceAll("");
        cleaned = EDITION_SUFFIX.matcher(cleaned).replaceAll("");
        cleaned = DISC_SUFFIX.matcher(cleaned).replaceAll("");
        return cleaned.trim().isEmpty() ? title.trim() : cleaned.trim();
    }

    /** Clave de comparación: ignora tildes, mayúsculas, puntuación y numeración de partes. */
    static String normalize(String title) {
        String cleaned = display(title);
        String withoutAccents = DIACRITICS
                .matcher(Normalizer.normalize(cleaned, Normalizer.Form.NFD))
                .replaceAll("");
        String flattened = NON_ALPHANUMERIC.matcher(withoutAccents.toLowerCase()).replaceAll(" ");
        return PART_WORD.matcher(flattened).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    /** Descarta singles, remixes y todo lo que no tenga portada propia. */
    static boolean isAlbum(AlbumEntry album, String imageUrl) {
        if (album.name() == null || album.name().isBlank() || imageUrl == null) {
            return false;
        }
        return !NON_ALBUM.matcher(album.name()).find();
    }

    static long playcountOf(AlbumEntry album) {
        try {
            return Long.parseLong(album.playcount());
        } catch (Exception e) {
            return 0;
        }
    }
}
