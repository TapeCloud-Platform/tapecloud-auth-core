package com.tapecloud.auth.content.api;

import com.tapecloud.auth.content.dto.ContentItemRequest;
import com.tapecloud.auth.content.entity.ContentItem;
import com.tapecloud.auth.content.service.ContentItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content")
public class ContentItemController {

    private final ContentItemService service;

    public ContentItemController(ContentItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ContentItem> find(
            @RequestParam(required = false) String sourceApp,
            @RequestParam(required = false) String sourceType) {
        return service.find(sourceApp, sourceType);
    }

    @GetMapping("/paginated")
    public Page<ContentItem> findPaginated(
            @RequestParam(required = false) String sourceApp,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return service.findPaginated(sourceApp, sourceType, genre, PageRequest.of(page, limit));
    }

    // 204 en vez de 404: la ausencia es un resultado válido y así el navegador no la registra como error.
    @GetMapping("/lookup")
    public ResponseEntity<ContentItem> lookup(
            @RequestParam String sourceApp,
            @RequestParam(defaultValue = "movie") String sourceType,
            @RequestParam String externalId) {
        return service.findByExternalIdOptional(sourceApp, sourceType, externalId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ContentItem findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContentItem save(@Valid @RequestBody ContentItemRequest request) {
        return service.save(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.deleteById(id);
    }
}
