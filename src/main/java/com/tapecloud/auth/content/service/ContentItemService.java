package com.tapecloud.auth.content.service;

import com.tapecloud.auth.content.dto.ContentItemRequest;
import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.repository.ContentItemRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContentItemService {

    private final ContentItemRepository repository;

    public ContentItemService(ContentItemRepository repository) {
        this.repository = repository;
    }

    public List<ContentItem> find(String sourceApp, String sourceType) {
        if (sourceApp == null || sourceApp.isBlank()) {
            return repository.findAll();
        }
        if (sourceType == null || sourceType.isBlank()) {
            return repository.findAllBySourceAppOrderByUpdatedAtDesc(sourceApp);
        }
        return repository.findAllBySourceAppAndSourceTypeOrderByUpdatedAtDesc(sourceApp, sourceType);
    }

    // Búsqueda con paginación
    public Page<ContentItem> findPaginated(String sourceApp, String sourceType, String genre, Pageable pageable) {
        if (genre != null && !genre.isBlank()) {
            if (sourceType == null || sourceType.isBlank()) {
                return repository.findBySourceAppAndGenreContainingIgnoreCaseOrderByUpdatedAtDesc(sourceApp, genre, pageable);
            }
            return repository.findBySourceAppAndSourceTypeAndGenreContainingIgnoreCaseOrderByUpdatedAtDesc(sourceApp, sourceType, genre, pageable);
        }

        if (sourceType == null || sourceType.isBlank()) {
            return repository.findBySourceAppOrderByUpdatedAtDesc(sourceApp, pageable);
        }
        return repository.findBySourceAppAndSourceTypeOrderByUpdatedAtDesc(sourceApp, sourceType, pageable);
    }

    public ContentItem findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contenido no encontrado"));
    }

    public java.util.Optional<ContentItem> findByExternalIdOptional(String sourceApp, String sourceType, String externalId) {
        return repository.findBySourceAppAndSourceTypeAndExternalId(sourceApp, sourceType, externalId);
    }

    // Este endpoint es público para cualquier usuario autenticado porque los frontends lo usan
    // para "registrar" contenido descubierto en vivo (TMDB/Last.fm) la primera vez que alguien
    // lo reseña. Por eso NO debe sobrescribir un registro existente con lo que mande el cliente
    // (ver hallazgo S-05): si ya existe, se devuelve tal cual está. Editar contenido existente
    // es una operación aparte, reservada a administradores.
    public ContentItem save(ContentItemRequest request) {
        return repository.findBySourceAppAndSourceTypeAndExternalId(
                request.sourceApp(), request.sourceType(), request.externalId())
            .orElseGet(() -> repository.save(new ContentItem(
                request.sourceApp(), request.sourceType(), request.externalId(), request.title(),
                request.description(), request.imageUrl(), request.releaseDate(), request.genre())));
    }

    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contenido no encontrado");
        }
        repository.deleteById(id);
    }
}
