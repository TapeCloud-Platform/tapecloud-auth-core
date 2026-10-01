package com.tapecloud.auth.api;

import com.tapecloud.auth.review.repository.ReviewRepository;
import com.tapecloud.auth.user.repository.AppUserRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Búsqueda pública de usuarios para el header de las apps.
 * Solo expone nombre visible + avatar: nunca email ni datos sensibles.
 */
@RestController
@RequestMapping("/api/users")
public class UserSearchController {

    private final AppUserRepository users;
    private final ReviewRepository reviews;

    public UserSearchController(AppUserRepository users, ReviewRepository reviews) {
        this.users = users;
        this.reviews = reviews;
    }

    public record PublicUser(String username, String displayName, String avatarDataUri) {
    }

    @GetMapping("/search")
    public List<PublicUser> search(@RequestParam String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) {
            return List.of();
        }
        return users.findTop10ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query)
                .stream()
                .map(u -> new PublicUser(
                        u.getUsername(),
                        u.getDisplayName() != null && !u.getDisplayName().isBlank()
                                ? u.getDisplayName()
                                : u.getUsername(),
                        u.getAvatarDataUri()))
                .toList();
    }

    public record PublicProfile(
            String username, String displayName, String avatarDataUri,
            long tapeflixReviews, long tapebeatReviews, String mostLikedReviewTitle) {
    }

    /** Perfil público de un usuario: stats de reseñas sin exponer email. */
    @GetMapping("/{username}/profile")
    public PublicProfile profile(@PathVariable String username) {
        var user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String email = user.getEmail();
        var top = reviews.findTopByAuthorEmailOrderByLikesDesc(email, PageRequest.of(0, 1));
        return new PublicProfile(
                user.getUsername(),
                user.getDisplayName() != null && !user.getDisplayName().isBlank()
                        ? user.getDisplayName()
                        : user.getUsername(),
                user.getAvatarDataUri(),
                reviews.countByAuthorEmailAndContentSourceApp(email, "tapeflix"),
                reviews.countByAuthorEmailAndContentSourceApp(email, "tapebeat"),
                top.isEmpty() ? null : top.get(0).getTitle());
    }
}
