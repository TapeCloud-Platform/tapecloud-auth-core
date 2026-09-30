package com.tapecloud.auth.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ContentItemRequest(
        @NotBlank @Size(max = 30) String sourceApp,
        @NotBlank @Size(max = 30) String sourceType,
        @NotBlank @Size(max = 120) String externalId,
        @NotBlank @Size(max = 250) String title,
        @Size(max = 4000) String description,
        @Size(max = 1000) String imageUrl,
        LocalDate releaseDate,
        @Size(max = 250) String genre
) {
}
