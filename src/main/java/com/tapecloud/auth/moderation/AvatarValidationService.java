package com.tapecloud.auth.moderation;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Validación de imágenes subidas (hoy: avatar de perfil).
 *
 * <p>Filtra por tipo MIME permitido, firma binaria real (magic bytes, no solo
 * la extensión), peso máximo en bytes y dimensiones mínimas/máximas. Esto
 * limita la subida de archivos pesados o con contenido inesperado.
 *
 * <p>Detección de contenido explícito (NSFW): sin proveedor externo no se puede
 * analizar el contenido semántico. El hook está previsto con
 * {@code moderation.image-provider} (ej. {@code sightengine}) y
 * {@code moderation.image-api-key}; mientras el proveedor sea {@code none}
 * solo se aplican los controles de tipo/tamaño/dimensiones.
 */
@Service
public class AvatarValidationService {

    private static final Logger log = LoggerFactory.getLogger(AvatarValidationService.class);

    private final long maxBytes;
    private final List<String> allowedTypes;
    private final int maxDimension;
    private final int minDimension;
    private final String imageProvider;
    private final String imageApiKey;

    public AvatarValidationService(
            @Value("${app.avatar.max-bytes:524288}") long maxBytes,
            @Value("${app.avatar.allowed-types:image/png,image/jpeg,image/webp,image/gif}") String allowedTypes,
            @Value("${app.avatar.max-dimension:2048}") int maxDimension,
            @Value("${app.avatar.min-dimension:32}") int minDimension,
            @Value("${moderation.image-provider:none}") String imageProvider,
            @Value("${moderation.image-api-key:}") String imageApiKey) {
        this.maxBytes = maxBytes;
        this.allowedTypes = Arrays.stream(allowedTypes.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList();
        this.maxDimension = maxDimension;
        this.minDimension = minDimension;
        this.imageProvider = imageProvider;
        this.imageApiKey = imageApiKey;
    }

    /**
     * Valida un data URI de imagen. Acepta {@code null}/vacío (borrar avatar).
     * Lanza {@link IllegalArgumentException} (400) si no cumple.
     */
    public void validate(String dataUri) {
        if (dataUri == null || dataUri.isBlank()) {
            return;
        }
        int comma = dataUri.indexOf(',');
        if (comma < 0 || !dataUri.startsWith("data:")) {
            throw new IllegalArgumentException("La imagen no es válida");
        }
        String header = dataUri.substring(5, comma).toLowerCase(Locale.ROOT);
        String[] headerParts = header.split(";");
        if (headerParts.length != 2 || !"base64".equals(headerParts[1].trim())) {
            throw new IllegalArgumentException("La imagen no es válida");
        }
        String mime = headerParts[0].trim();
        if (!allowedTypes.contains(mime)) {
            throw new IllegalArgumentException(
                    "Tipo de imagen no permitido. Usá PNG, JPEG, WEBP o GIF.");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1).trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La imagen no es válida");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("La imagen no es válida");
        }
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "La imagen es demasiado pesada (máximo " + (maxBytes / 1024) + " KB).");
        }

        String detected = detectMime(bytes);
        if (detected == null || !detected.equals(mime) && !equivalentMime(detected, mime)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }

        checkDimensions(bytes, mime);

        if (!"none".equalsIgnoreCase(imageProvider)) {
            // Hook para proveedor externo de moderación (Sightengine, etc).
            // Sin API key configurada no se puede analizar: se avisa en el log.
            if (imageApiKey == null || imageApiKey.isBlank()) {
                log.warn("Proveedor de moderación '{}' sin API key; se omite el análisis NSFW", imageProvider);
            }
        }
    }

    /** jpeg/jpg son el mismo formato. */
    private static boolean equivalentMime(String detected, String declared) {
        return (detected.equals("image/jpeg") && declared.equals("image/jpg"))
                || (detected.equals("image/jpg") && declared.equals("image/jpeg"));
    }

    /** Detecta el tipo real por magic bytes. */
    static String detectMime(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 6 && bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) {
            return "image/gif";
        }
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) {
            return "image/webp";
        }
        return null;
    }

    private void checkDimensions(byte[] bytes, String mime) {
        // ImageIO estándar no lee WEBP: para ese formato solo se controla tipo y peso.
        if ("image/webp".equals(mime)) {
            return;
        }
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("El archivo no es una imagen válida");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < minDimension || height < minDimension) {
                    throw new IllegalArgumentException(
                            "La imagen es demasiado chica (mínimo " + minDimension + "x" + minDimension + " px).");
                }
                if (width > maxDimension || height > maxDimension) {
                    throw new IllegalArgumentException(
                            "La imagen es demasiado grande (máximo " + maxDimension + "x" + maxDimension + " px).");
                }
            } finally {
                reader.dispose();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("El archivo no es una imagen válida");
        }
    }
}
